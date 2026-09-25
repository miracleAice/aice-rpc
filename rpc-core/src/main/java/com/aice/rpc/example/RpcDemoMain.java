package com.aice.rpc.example;

import com.aice.rpc.client.RpcClient;
import com.aice.rpc.client.RpcClientManager;
import com.aice.rpc.client.RpcProxyUtil;
import com.aice.rpc.loadbalance.LoadBalancerManager;
import com.aice.rpc.registry.InMemoryServiceInstanceDiscovery;
import com.aice.rpc.registry.ServiceInstance;
import com.aice.rpc.registry.ServiceInstanceDiscovery;
import com.aice.rpc.server.RpcServer;
import com.aice.rpc.service.CalculatorService;

/**
 * 教学版 RPC 调用示例。
 *
 * @author aice Cheng
 */
public class RpcDemoMain {
    /**
     * 在此方法中手动组装服务端和客户端，完成一次 RPC 调用。
     *
     * @param args 命令行参数，本示例不使用
     * @throws InterruptedException 等待服务端线程结束时发生中断
     */
    public static void main(String[] args) throws InterruptedException {
        // 定义服务端监听端口，并创建 RpcServer。
        RpcServer serverA = new RpcServer(8080);
        RpcServer serverB = new RpcServer(8081);
        RpcServer serverC = new RpcServer(8082);
        RpcClientManager rpcClientManager = new RpcClientManager();

        // 使用独立线程启动服务端，因为 RpcServer.start 会持续监听端口。
        Thread tA = new Thread(serverA::start);
        Thread tB = new Thread(serverB::start);
        Thread tC = new Thread(serverC::start);
        tA.start();
        tB.start();
        tC.start();

        try {
            // 等待服务端完成端口监听后，再创建客户端 TCP 连接。
            Thread.sleep(100);

            // 创建共享的服务发现和负载均衡管理对象。
            ServiceInstanceDiscovery serviceInstanceDiscovery = new InMemoryServiceInstanceDiscovery();
            LoadBalancerManager loadBalancerManager = new LoadBalancerManager();

            // 创建 ServiceInstance。
            ServiceInstance serviceInstanceA = new ServiceInstance("localhost", 8080);
            ServiceInstance serviceInstanceB = new ServiceInstance("localhost", 8081);
            ServiceInstance serviceInstanceC = new ServiceInstance("localhost", 8082);

            // 将 CalculatorService 的接口名和三个服务实例注册到服务发现组件。
            serviceInstanceDiscovery.register(CalculatorService.class.getName(), serviceInstanceA);
            serviceInstanceDiscovery.register(CalculatorService.class.getName(), serviceInstanceB);
            serviceInstanceDiscovery.register(CalculatorService.class.getName(), serviceInstanceC);

            // 服务端开始监听后，创建与各服务实例对应的 RpcClient，并保存到客户端管理器。
            RpcClient clientA = new RpcClient("localhost", 8080);
            RpcClient clientB = new RpcClient("localhost", 8081);
            RpcClient clientC = new RpcClient("localhost", 8082);
            rpcClientManager.setClient(serviceInstanceA, clientA);
            rpcClientManager.setClient(serviceInstanceB, clientB);
            rpcClientManager.setClient(serviceInstanceC, clientC);

            // 创建 RpcProxyUtil 和 CalculatorService 代理，通过代理调用 add 方法。
            RpcProxyUtil rpcProxyUtil = new RpcProxyUtil(rpcClientManager, serviceInstanceDiscovery, loadBalancerManager);
            CalculatorService calculatorProxy = rpcProxyUtil.getProxy(CalculatorService.class);
            System.out.println(calculatorProxy.add(1, 2));
        } finally {
            // 先关闭全部客户端连接，再停止服务端并等待三个服务端线程退出。
            try {
                rpcClientManager.close();
            } finally {
                serverA.stop();
                serverB.stop();
                serverC.stop();
                tA.join();
                tB.join();
                tC.join();
            }
        }

    }
}
