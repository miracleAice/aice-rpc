package com.aice.rpc.loadbalance;

import com.aice.rpc.registry.ServiceInstance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询（RR）负载均衡器，从全部实例中依次分配任务
 *
 * @author aice Cheng
 * Created on  2026/9/25 15:16
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
    // 每个负载均衡器对象独立维护轮询位置，避免不同服务的选择过程互相影响。
    private final AtomicInteger index = new AtomicInteger(0);

    /**
     * 按实例列表顺序循环选择一个服务实例。
     *
     * @param instances 服务发现返回的可用服务实例列表
     * @return 本次轮询选中的服务实例
     */
    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            throw new IllegalArgumentException("没有可用于负载均衡的服务实例");
        }

        int instanceSize = instances.size();
        while (true) {
            int currentIndex = index.get();
            // 候选列表缩小时，先把旧下标校正到当前列表范围内。
            int selectedIndex = Math.floorMod(currentIndex, instanceSize);
            int nextIndex = (selectedIndex + 1) % instanceSize;
            // 只有成功更新轮询位置的线程才能使用本次选中的下标。
            if (index.compareAndSet(currentIndex, nextIndex)) {
                return instances.get(selectedIndex);
            }
        }
    }
}
