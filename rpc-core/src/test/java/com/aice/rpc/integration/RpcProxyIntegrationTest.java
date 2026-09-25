package com.aice.rpc.integration;

import com.aice.rpc.client.RpcClient;
import com.aice.rpc.client.RpcClientManager;
import com.aice.rpc.client.RpcProxyUtil;
import com.aice.rpc.loadbalance.LoadBalancerManager;
import com.aice.rpc.registry.InMemoryServiceInstanceDiscovery;
import com.aice.rpc.registry.ServiceInstance;
import com.aice.rpc.registry.ServiceInstanceDiscovery;
import com.aice.rpc.service.CalculatorService;
import com.aice.rpc.server.RpcServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证通过动态代理发起远程调用的集成测试。
 *
 * @author aice Cheng
 */
class RpcProxyIntegrationTest {

    /**
     * 验证调用代理对象的方法时，能够自动完成 RPC 请求构造、网络传输和结果返回。
     */
    @Test
    void shouldInvokeRemoteMethodThroughProxy() throws InterruptedException {
        int port = 8081;
        RpcServer server = new RpcServer(port);
        RpcClient client = null;

        // RpcServer.start 会阻塞当前线程，因此在独立线程中启动服务端。
        Thread serverThread = new Thread(server::start);
        serverThread.start();

        try {
            // 暂时等待服务端完成端口监听；后续可使用 CountDownLatch 替代固定等待。
            Thread.sleep(100);

            // 注册表、客户端管理器和负载均衡器由本测试统一创建，并作为客户端调用链的共享对象。
            ServiceInstanceDiscovery discovery = new InMemoryServiceInstanceDiscovery();
            RpcClientManager clientManager = new RpcClientManager();
            LoadBalancerManager loadBalancerManager = new LoadBalancerManager();

            // 注册服务端对外暴露的网络地址，使客户端能够通过接口名发现该服务实例。
            ServiceInstance instance = new ServiceInstance("localhost", port);
            discovery.register(CalculatorService.class.getName(), instance);

            // 服务端已开始监听后，建立连接并缓存到客户端管理器，供代理调用时复用。
            client = new RpcClient(instance.getHost(), instance.getPort());
            clientManager.setClient(instance, client);

            // 创建客户端代理，并传入服务发现、负载均衡和连接管理所需的共享对象。
            RpcProxyUtil rpcProxyUtil = new RpcProxyUtil(clientManager, discovery, loadBalancerManager);
            // 通过 CalculatorService.class 创建 CalculatorService 的远程代理对象。
            CalculatorService calculatorServiceProxy = rpcProxyUtil.getProxy(CalculatorService.class);
            // 像调用本地对象一样调用代理对象的 add(1, 2) 方法，并保存返回值。
            int result = calculatorServiceProxy.add(1, 2);
            // 断言远程调用结果为 3。
            assertEquals(3, result);
        } finally {
            // 无论测试成功或失败，都停止服务端并等待服务端线程结束，避免端口和线程残留。
            if (client != null) {
                client.close();
            }
            server.stop();
            serverThread.join();
        }
    }
}
