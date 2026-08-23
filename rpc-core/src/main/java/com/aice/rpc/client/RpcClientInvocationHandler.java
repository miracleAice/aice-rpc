package com.aice.rpc.client;

import com.aice.rpc.protocol.RpcMessage;
import com.aice.rpc.protocol.RpcRequest;
import com.aice.rpc.protocol.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * RPC 客户端动态代理调用处理器。
 * 该类后续负责接收接口方法调用，并将其转换为远程 RPC 请求。
 *
 * @author aice Cheng
 */
public class RpcClientInvocationHandler implements InvocationHandler {
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
        String interfaceName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        Object[] parameterValues = args;
        Class<?>[] parameterTypes = method.getParameterTypes();

        RpcRequest rpcRequest = new RpcRequest(interfaceName, methodName, parameterValues, parameterTypes);
        String requestId = UUID.randomUUID().toString();
        RpcMessage requestMessage = new RpcMessage((byte)1, requestId, rpcRequest);

        RpcMessage responseMessage = rpcClient.send(requestMessage);
        if (responseMessage == null) {
            throw new RuntimeException("服务端未返回消息");
        }
        if (responseMessage.getMessageType() != 2) {
            throw new RuntimeException("服务端返回的不是响应消息");
        }
        if (!requestId.equals(responseMessage.getRequestId())) {
            throw new RuntimeException("响应与请求不匹配");
        }
        if (!(responseMessage.getData() instanceof RpcResponse)) {
            throw new RuntimeException("消息体类型不正确");
        }
        RpcResponse rpcResponse = (RpcResponse)responseMessage.getData();
        if (rpcResponse.getStatus() == RpcResponse.FAILURE) {
            throw new RuntimeException(rpcResponse.getErrorMessage());
        }

        return rpcResponse.getReturnValue();
    }
}
