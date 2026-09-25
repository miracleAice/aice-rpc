package com.aice.rpc.client;

import com.aice.rpc.loadbalance.LoadBalancer;
import com.aice.rpc.loadbalance.LoadBalancerManager;
import com.aice.rpc.exception.RpcException;
import com.aice.rpc.exception.RpcRemoteException;
import com.aice.rpc.protocol.RpcMessage;
import com.aice.rpc.protocol.RpcRequest;
import com.aice.rpc.protocol.RpcResponse;
import com.aice.rpc.registry.ServiceInstance;
import com.aice.rpc.registry.ServiceInstanceDiscovery;
import com.aice.rpc.retry.AiceFirstRetryPolicy;
import com.aice.rpc.retry.RetryPolicy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 客户端动态代理调用处理器。
 * 该类后续负责接收接口方法调用，并将其转换为远程 RPC 请求。
 *
 * @author aice Cheng
 */
public class RpcClientInvocationHandler implements InvocationHandler {
    private static final AtomicLong REQUEST_ID_GENERATOR = new AtomicLong(0);
    private final RpcClientManager rpcClientManager;
    private final ServiceInstanceDiscovery discovery;
    private final LoadBalancerManager loadBalancerManager;

    private final RetryPolicy retryPolicy = new AiceFirstRetryPolicy();

    public RpcClientInvocationHandler(RpcClientManager rpcClientManager,
                                      ServiceInstanceDiscovery discovery,
                                      LoadBalancerManager loadBalancerManager) {
        this.rpcClientManager = rpcClientManager;
        this.discovery = discovery;
        this.loadBalancerManager = loadBalancerManager;
    }

    /**
     * 拦截代理对象上的接口方法调用。
     * 构造 RPC 请求、发送请求并返回远程调用结果。
     *
     * @param proxy 当前代理对象
     * @param method 被调用的接口方法
     * @param args 方法实参；无参方法时可能为 null
     * @return 远程调用结果
     * @throws Throwable 调用过程中发生的异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args){
        // toString、hashCode 和 equals 也会被动态代理拦截。
        // 它们不属于远程服务方法，因此在本地处理，避免向服务端发送 java.lang.Object 的调用请求。
        if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
                case "toString" -> {
                    return "RPC 代理对象：" + proxy.getClass().getInterfaces()[0].getName();
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "equals" -> {
                    return proxy == args[0];
                }
            }
        }

        // ============== 消息组装阶段 ==============
        /*
          从被拦截的方法和实际参数中提取远程调用所需的信息：
          1. 服务接口全限定名（com.aice.rpc.example.service.CalculatorService）
          2. 方法名
          3. 参数实际值
          4. 参数类型
        */
        String interfaceName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        Object[] parameterValues = args;
        Class<?>[] parameterTypes = method.getParameterTypes();
        RpcRequest rpcRequest = new RpcRequest(interfaceName, methodName, parameterValues, parameterTypes);
        // 为本次调用生成递增请求标识，并将请求体封装为外层 RPC 消息。
        long requestId = REQUEST_ID_GENERATOR.incrementAndGet();
        RpcMessage requestMessage = new RpcMessage(
                RpcMessage.VERSION_1,
                RpcMessage.SERIALIZER_JDK,
                RpcMessage.MESSAGE_REQUEST,
                requestId,
                RpcMessage.STATUS_SUCCESS,
                0,
                rpcRequest
        );

        // ============== 发送阶段 ==============
        // 根据 serviceName 进行服务发现
        List<ServiceInstance> instanceList = discovery.discover(interfaceName);
        // 每次调用使用独立的可修改列表，避免并发调用互相覆盖，并允许排除失败节点。
        List<ServiceInstance> attemptInstanceList = new ArrayList<>(instanceList);
        // 获取当前服务独立使用的负载均衡器，再从可尝试服务实例列表中选择实例。
        LoadBalancer balancer = loadBalancerManager.getRoundRobinLoadBalancer(interfaceName);

        // RpcClient 发送调用请求，且在发生允许重试的错误时进行重试
        RpcMessage responseMessage = null;
        RpcException lastException = null;
        for (int retryTimes = 0; retryTimes <= retryPolicy.getMaxRetryTimes(); retryTimes++) {
            ServiceInstance instance = balancer.select(attemptInstanceList);
            try {
                // 根据服务实例获取对应的 RpcClient；获取失败也属于本次可判断的 RPC 异常。
                RpcClient rpcClient = rpcClientManager.getClient(instance);
                responseMessage = rpcClient.send(requestMessage);
                break;
            } catch (RpcException exception) {
                lastException = exception;
                boolean reachedRetryLimit = retryTimes >= retryPolicy.getMaxRetryTimes();
                if (!retryPolicy.shouldRetry(exception) || reachedRetryLimit) {
                    throw exception;
                }

                // 排除本次失败节点，确保下一次尝试会选择其他服务实例。
                attemptInstanceList.remove(instance);
                if (attemptInstanceList.isEmpty()) {
                    throw exception;
                }
            }
        }

        // 校验响应是否属于本次调用，且响应消息结构符合预期。
        if (responseMessage == null) {
            throw lastException != null ? lastException : new RpcException("服务端未返回消息");
        }
        if (responseMessage.getMessageType() != RpcMessage.MESSAGE_RESPONSE) {
            throw new RpcException("服务端返回的不是响应消息");
        }
        if (requestId != responseMessage.getRequestId()) {
            throw new RpcException("响应与请求不匹配");
        }
        if (!(responseMessage.getBody() instanceof RpcResponse)) {
            throw new RpcException("消息体类型不正确");
        }
        // 类型校验通过后，安全取得 RPC 响应体。
        RpcResponse rpcResponse = (RpcResponse)responseMessage.getBody();
        // 服务端返回失败或未知状态时，向接口调用方抛出异常。
        if (rpcResponse.getStatus() != RpcResponse.SUCCESS) {
            throw new RpcRemoteException(rpcResponse.getErrorMessage());
        }
        // 服务端调用成功时，将远程返回值作为接口方法返回值。
        return rpcResponse.getReturnValue();
    }
}
