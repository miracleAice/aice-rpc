package com.aice.rpc.registry;

import java.util.List;

/**
 * @author aice Cheng
 * Created on  2026/9/25 14:16
 */
public interface ServiceInstanceDiscovery {
    /**
     * 注册服务实例
     */
    void register(String serviceName, ServiceInstance instance);

    /**
    * 服务实例发现
    * */
    List<ServiceInstance> discover(String serviceName);
}
