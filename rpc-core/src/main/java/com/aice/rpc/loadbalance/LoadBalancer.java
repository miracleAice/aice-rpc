package com.aice.rpc.loadbalance;

import com.aice.rpc.registry.ServiceInstance;

import java.util.List;

/**
 * 负载均衡器接口
 *
 * @author aice Cheng
 * Created on  2026/9/25 15:08
 */
public interface LoadBalancer {
    ServiceInstance select(List<ServiceInstance> instances);
}
