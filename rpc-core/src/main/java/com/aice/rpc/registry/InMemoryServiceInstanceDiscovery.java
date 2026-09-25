package com.aice.rpc.registry;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 利用内存记录并实现服务发现
 *
 * @author aice Cheng
 * Created on  2026/9/25 14:18
 */
public class InMemoryServiceInstanceDiscovery implements ServiceInstanceDiscovery {
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ServiceInstance>> serviceMap;
    private static final Logger log = LoggerFactory.getLogger(InMemoryServiceInstanceDiscovery.class);

    public InMemoryServiceInstanceDiscovery() {
        this.serviceMap = new ConcurrentHashMap<>();
    }

    @Override
    public void register(String serviceName, ServiceInstance instance) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("用于服务注册的 serviceName 为空");
        }
        if (instance == null) {
            throw new IllegalArgumentException("用于服务注册的 instance 为空");
        }
        if (instance.getHost().isBlank()) {
            throw new IllegalArgumentException("用于服务注册的 host 为空");
        }
        if (instance.getPort() < 1 || instance.getPort() > 65535) {
            throw new IllegalArgumentException("用于服务注册的 port 不合法");
        }

        // 原子地创建同一服务对应的实例列表，避免并发首次注册时覆盖其他线程已加入的实例。
        serviceMap.computeIfAbsent(serviceName, key -> new CopyOnWriteArrayList<>())
                // 相同 host 和 port 的实例只保留一份，避免重复注册。
                .addIfAbsent(instance);
    }

    @Override
    public List<ServiceInstance> discover(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("用于服务发现的 serviceName 为空");
        }
        List<ServiceInstance> instances = serviceMap.get(serviceName);
        if (instances == null || instances.isEmpty()) {
            log.warn("服务 {} 没有可用实例", serviceName);
            // 返回空列表而不是 null，调用方可以直接遍历或调用 isEmpty。
            return List.of();
        }
        // 返回列表副本，避免调用方修改注册表中的实例列表。
        return List.copyOf(instances);
    }
}
