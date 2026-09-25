package com.aice.rpc.registry;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 利用内存记录并实现服务发现
 *
 * @author aice Cheng
 * Created on  2026/9/25 14:18
 */
public class InMemoryServiceDiscovery implements ServiceDiscovery{
    private final Map<String, List<ServiceInstance>> serviceMap;
    private static final Logger log = LoggerFactory.getLogger(InMemoryServiceDiscovery.class);

    public InMemoryServiceDiscovery(){
        this.serviceMap = new HashMap<>();
    }

    @Override
    public void register(String serviceName, ServiceInstance instance){
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("用于服务注册的 serviceName 为空");
        }
        List<ServiceInstance> instances = serviceMap.getOrDefault(serviceName, new ArrayList<>());
        instances.add(instance);
        serviceMap.put(serviceName, instances);
    }

    @Override
    public List<ServiceInstance> discover(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("用于服务发现的 serviceName 为空");
        }
        if (serviceMap.get(serviceName).isEmpty()) {
            log.warn("服务 {} 没有可用实例", serviceName);
            return null;
        }
        return serviceMap.get(serviceName);
    }
}
