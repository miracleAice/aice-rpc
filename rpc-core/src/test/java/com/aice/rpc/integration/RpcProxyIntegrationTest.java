package com.aice.rpc.integration;

import com.aice.rpc.client.RpcClient;
import com.aice.rpc.client.RpcProxyUtil;
import com.aice.rpc.example.service.CalculatorService;
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
        RpcServer server = new RpcServer(8081);
        RpcClient client = new RpcClient("localhost", 8081);

        // RpcServer.start 会阻塞当前线程，因此在独立线程中启动服务端。
        Thread serverThread = new Thread(server::start);
        serverThread.start();

        try {
            // 暂时等待服务端完成端口监听；后续可使用 CountDownLatch 替代固定等待。
            Thread.sleep(100);

            // 创建 RpcProxyUtil，并传入 rpcClient。
            RpcProxyUtil rpcProxyUtil = new RpcProxyUtil(client);
            // 通过 CalculatorService.class 创建 CalculatorService 的远程代理对象。
            CalculatorService calculatorServiceProxy = rpcProxyUtil.getProxy(CalculatorService.class);
            // 像调用本地对象一样调用代理对象的 add(1, 2) 方法，并保存返回值。
            int result = calculatorServiceProxy.add(1, 2);
            // 断言远程调用结果为 3。
            assertEquals(3, result);
        } finally {
            // 无论测试成功或失败，都停止服务端并等待服务端线程结束，避免端口和线程残留。
            server.stop();
            serverThread.join();
        }
    }
}
