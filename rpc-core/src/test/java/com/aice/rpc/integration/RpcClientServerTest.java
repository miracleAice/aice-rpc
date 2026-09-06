package com.aice.rpc.integration;

import com.aice.rpc.client.RpcClient;
import com.aice.rpc.example.service.CalculatorService;
import com.aice.rpc.protocol.RpcMessage;
import com.aice.rpc.protocol.RpcRequest;
import com.aice.rpc.protocol.RpcResponse;
import com.aice.rpc.server.RpcServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 RpcClient 与 RpcServer 之间网络请求和响应的集成测试。
 *
 * @author aice Cheng
 */
class RpcClientServerTest {

    /**
     * 验证客户端发送请求后，能够接收到服务端返回的响应消息。
     */
    @Test
    void shouldSendRequestAndReceiveResponse() throws InterruptedException {
        // 创建本地服务端，用于验证客户端和服务端之间的真实 Socket 通信。
        RpcServer server = new RpcServer(8080);

        // 客户端连接到与服务端相同的地址和端口。
        RpcClient client = new RpcClient("localhost", 8080);

        // 请求使用接口全限定名、方法名、参数值和参数类型，准确描述 CalculatorService.add(int, int) 调用。
        RpcRequest testRequest = new RpcRequest(CalculatorService.class.getName(), "add",
                                                 new Object[]{1, 2}, new Class<?>[]{int.class, int.class});
        // 构造完整请求消息，requestId 用于验证服务端响应是否属于本次请求。
        RpcMessage message = new RpcMessage(RpcMessage.MESSAGE_REQUEST, 123L, testRequest);

        // RpcServer.start 会阻塞当前线程，因此必须在独立线程中启动服务端。
        Thread serverThread = new Thread(server::start);
        serverThread.start();

        try {
            // 暂时等待服务端完成端口监听，避免客户端在服务端尚未启动时发起连接。
            // 后续可使用 CountDownLatch 等启动通知机制替代固定等待时间。
            Thread.sleep(100);

            // 发送请求并同步等待服务端返回响应消息。
            RpcMessage result = client.send(message);

            // 服务端返回的外层消息必须是响应类型，并且沿用原始 requestId。
            assertEquals(RpcMessage.MESSAGE_RESPONSE, result.getMessageType());
            assertEquals(123L, result.getRequestId());

            // 响应消息的 body 应为 RpcResponse，assertInstanceOf 会验证类型并完成转换。
            RpcResponse response = assertInstanceOf(
                    RpcResponse.class,
                    result.getBody()
            );

            // 验证服务端已实际调用 add(1, 2)，并将调用结果封装为成功响应。
            assertEquals(RpcResponse.SUCCESS, response.getStatus());
            assertEquals(3, response.getReturnValue());
            assertNull(response.getErrorMessage());

        } finally {
            // 无论断言成功或失败都停止服务端，避免后台线程和监听端口遗留到后续测试。
            server.stop();

            // 等待服务端线程真正结束，确保端口已经释放。
            serverThread.join();
        }
    }
}
