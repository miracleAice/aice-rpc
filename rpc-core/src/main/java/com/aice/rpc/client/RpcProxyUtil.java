package com.aice.rpc.client;

import java.lang.reflect.Proxy;

/**
 * RPC 客户端动态代理工具类。
 * 负责将对特定接口方法的调用转换为远程 RPC 请求。
 *
 * @author aice Cheng
 */
public class RpcProxyUtil {

    private final RpcClient rpcClient;

    public RpcProxyUtil(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public <T> T getProxy(Class<T> target) {
        if (!target.isInterface()) {
            throw new IllegalArgumentException("动态代理目标必须是接口");
        }
        Object proxy = Proxy.newProxyInstance(
                        target.getClassLoader(),
                        new Class<?>[]{target},
                        new RpcClientInvocationHandler(this.rpcClient));
        return target.cast(proxy);
    }

}
