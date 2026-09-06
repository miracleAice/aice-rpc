package com.aice.rpc.client;

import com.aice.rpc.protocol.RpcMessage;
import com.aice.rpc.protocol.RpcRequest;
import com.aice.rpc.protocol.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 客户端动态代理调用处理器。
 * 该类后续负责接收接口方法调用，并将其转换为远程 RPC 请求。
 *
 * @author aice Cheng
 */
public class RpcClientInvocationHandler implements InvocationHandler {
    private static final AtomicLong REQUEST_ID_GENERATOR = new AtomicLong(0);

    private final RpcClient rpcClient;

    public RpcClientInvocationHandler(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
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
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        RpcMessage requestMessage = new RpcMessage(RpcMessage.MESSAGE_REQUEST, requestId, rpcRequest);
        RpcMessage responseMessage = rpcClient.send(requestMessage);
        // 校验响应是否属于本次调用，且响应消息结构符合预期。
        if (responseMessage == null) {
            throw new RuntimeException("服务端未返回消息");
        }
        if (responseMessage.getMessageType() != RpcMessage.MESSAGE_RESPONSE) {
            throw new RuntimeException("服务端返回的不是响应消息");
        }
        if (requestId != responseMessage.getRequestId()) {
            throw new RuntimeException("响应与请求不匹配");
        }
        if (!(responseMessage.getBody() instanceof RpcResponse)) {
            throw new RuntimeException("消息体类型不正确");
        }
        // 类型校验通过后，安全取得 RPC 响应体。
        RpcResponse rpcResponse = (RpcResponse)responseMessage.getBody();
        // 服务端返回失败或未知状态时，向接口调用方抛出异常。
        if (rpcResponse.getStatus() != RpcResponse.SUCCESS) {
            throw new RuntimeException(rpcResponse.getErrorMessage());
        }
        // 服务端调用成功时，将远程返回值作为接口方法返回值。
        return rpcResponse.getReturnValue();
    }
}
