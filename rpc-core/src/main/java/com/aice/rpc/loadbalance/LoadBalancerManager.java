package com.aice.rpc.loadbalance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 负载均衡器管理器，为每个服务接口维护独立的负载均衡器。
 *
 * @author aice Cheng
 */
public class LoadBalancerManager {
    private final ConcurrentMap<String, LoadBalancer> randomBalancers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LoadBalancer> roundRobinBalancers = new ConcurrentHashMap<>();

    /**
     * 获取指定服务独立使用的随机负载均衡器。
     *
     * @param serviceName 服务接口全限定名
     * @return 该服务对应的负载均衡器
     */
    public LoadBalancer getRandomLoadBalancer(String serviceName) {
        validateServiceName(serviceName);

        // 随机策略使用独立缓存，不与轮询策略共享同一个负载均衡器对象。
        return randomBalancers.computeIfAbsent(serviceName, key -> new RandomLoadBalancer());
    }

    /**
     * 获取指定服务独立使用的轮询负载均衡器。
     *
     * @param serviceName 服务接口全限定名
     * @return 该服务对应的负载均衡器
     */
    public LoadBalancer getRoundRobinLoadBalancer(String serviceName) {
        validateServiceName(serviceName);

        // 同一服务只创建一个轮询器，使该服务的轮询下标能够在多次调用间保留。
        return roundRobinBalancers.computeIfAbsent(serviceName, key -> new RoundRobinLoadBalancer());
    }

    /**
     * 校验服务接口全限定名。
     *
     * @param serviceName 服务接口全限定名
     */
    private void validateServiceName(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("服务名称不能为空");
        }
    }
}
