package com.aice.rpc.loadbalance;

import com.aice.rpc.registry.ServiceInstance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机负载均衡器，从可用服务实例中随机选择一个实例。
 *
 * @author aice Cheng
 * Created on  2026/9/25 15:09
 */
public class RandomLoadBalancer implements LoadBalancer {

    /**
     * 从服务实例列表中随机选择一个实例。
     *
     * @param instances 服务发现返回的可用服务实例列表
     * @return 被随机选中的服务实例
     */
    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            throw new IllegalArgumentException("没有可用于负载均衡的服务实例");
        }

        // 生成范围为 [0, instances.size()) 的随机下标，并返回对应服务实例。
        int randomIndex = ThreadLocalRandom.current().nextInt(instances.size());
        return instances.get(randomIndex);
    }
}
