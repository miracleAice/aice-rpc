package com.aice.rpc.client;

import com.aice.rpc.loadbalance.LoadBalancerManager;
import com.aice.rpc.registry.ServiceInstanceDiscovery;

import java.lang.reflect.Proxy;

/**
 * RPC 客户端动态代理工具类。
 * 负责将对特定接口方法的调用转换为远程 RPC 请求。
 *
 * @author aice Cheng
 */
public class RpcProxyUtil {

    private final RpcClientManager clientManager;
    private final ServiceInstanceDiscovery discovery;
    private final LoadBalancerManager loadBalancerManager;

    public RpcProxyUtil(RpcClientManager clientManager,
                        ServiceInstanceDiscovery discovery,
                        LoadBalancerManager loadBalancerManager) {
        if (clientManager == null) {
            throw new IllegalArgumentException("RpcClientManager 不能为空");
        }
        if (discovery == null) {
            throw new IllegalArgumentException("ServiceInstanceDiscovery 不能为空");
        }
        if (loadBalancerManager == null) {
            throw new IllegalArgumentException("LoadBalancerManager 不能为空");
        }

        this.clientManager = clientManager;
        this.discovery = discovery;
        this.loadBalancerManager = loadBalancerManager;
    }

    /**
     * 为指定接口创建 RPC 动态代理对象。
     *
     * @param target 需要代理的服务接口类型
     * @param <T> 服务接口类型
     * @return 实现目标接口的 RPC 代理对象
     */
    public <T> T getProxy(Class<T> target) {
        if (!target.isInterface()) {
            throw new IllegalArgumentException("动态代理目标必须是接口");
        }
        /*
         Proxy.newProxyInstance 的三个参数
          1.创建代理对象的类加载器 ClassLoader loader
          2.需要被代理的接口类型 Class<?>[] interfaces
          3.调用处理器 InvocationHandler，负责拦截接口方法调用并转发为 RPC 请求
          */
        Object proxy = Proxy.newProxyInstance(
                        target.getClassLoader(),
                        new Class<?>[]{target},
                        new RpcClientInvocationHandler(clientManager, discovery, loadBalancerManager));
        return target.cast(proxy);
    }

}
