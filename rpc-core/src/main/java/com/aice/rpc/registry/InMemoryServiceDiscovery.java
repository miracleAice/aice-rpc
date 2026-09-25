package com.aice.rpc.registry;

import java.util.List;

/**
 * 利用内存记录并实现服务发现
 *
 * @author aice Cheng
 * Created on  2026/9/25 14:18
 */
public class InMemoryServiceDiscovery implements ServiceDiscovery{
    @Override
    public void register(String serviceName, ServiceInstance instance){

    }

    @Override
    public List<ServiceInstance> discover(String serviceName) {
        return null;
    }
}
