package com.aice.rpc.integration;

import com.aice.rpc.client.RpcClient;
import com.aice.rpc.example.service.CalculatorService;
import com.aice.rpc.protocol.RpcDecoder;
import com.aice.rpc.protocol.RpcEncoder;
import com.aice.rpc.protocol.RpcMessage;
import com.aice.rpc.protocol.RpcRequest;
import com.aice.rpc.protocol.RpcResponse;
import com.aice.rpc.server.RpcServer;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 验证 RPC 服务端能够同时处理多个客户端连接。
 *
 * @author aice Cheng
 */
class RpcServerConcurrencyTest {

    /**
     * 验证同一个 RpcClient 连续发送两条请求时，服务端不会在第一条响应后关闭连接。
     */
    @Test
    void shouldSendTwoRequestsOverOneConnection() throws Exception {
        int port = findAvailablePort();
        RpcServer server = new RpcServer(port);
        Thread serverThread = new Thread(server::start);
        serverThread.start();

        try {
            // 等待服务端完成端口监听后再创建客户端连接。
            Thread.sleep(100);
            RpcClient client = new RpcClient("localhost", port);
            try {
                // 同一个 client 实例连续发送两次请求，第二次成功说明连接被复用。
                assertResponse(client.send(requestMessage(201L, 1, 2)), 201L, 3);
                assertResponse(client.send(requestMessage(202L, 3, 4)), 202L, 7);
            } finally {
                client.close();
            }
        } finally {
            // 无论断言是否成功，均停止服务端并等待监听线程退出。
            server.stop();
            serverThread.join();
        }
    }

    /**
     * 验证慢客户端阻塞在读取协议头时，其他客户端仍能完成 RPC 调用。
     */
    @Test
    void shouldProcessOtherClientsWhileSlowClientsBlock() throws Exception {
        int port = findAvailablePort();
        RpcServer server = new RpcServer(port);
        Thread serverThread = new Thread(server::start);
        ExecutorService clientExecutor = Executors.newFixedThreadPool(2);
        serverThread.start();

        try {
            // 等待服务端开始监听，随后建立两个暂不发送数据的慢连接。
            Thread.sleep(100);
            try (Socket slowClientOne = new Socket("localhost", port);
                 Socket slowClientTwo = new Socket("localhost", port)) {
                // 两个慢连接占用工作线程时，两个正常 RPC 仍应由其他工作线程及时完成。
                Future<RpcMessage> firstResponse = clientExecutor.submit(
                        () -> new RpcClient("localhost", port).send(requestMessage(101L, 1, 2)));
                Future<RpcMessage> secondResponse = clientExecutor.submit(
                        () -> new RpcClient("localhost", port).send(requestMessage(102L, 3, 4)));

                assertResponse(firstResponse.get(1, TimeUnit.SECONDS), 101L, 3);
                assertResponse(secondResponse.get(1, TimeUnit.SECONDS), 102L, 7);

                // 正常请求完成后补发完整报文，使慢连接也按正常 RPC 流程结束。
                assertResponse(completeSlowRequest(slowClientOne, requestMessage(103L, 5, 6)), 103L, 11);
                assertResponse(completeSlowRequest(slowClientTwo, requestMessage(104L, 7, 8)), 104L, 15);
            }
        } finally {
            // 无论断言是否成功，均关闭测试线程、服务端和监听线程。
            clientExecutor.shutdownNow();
            server.stop();
            serverThread.join();
        }
    }

    /**
     * 申请一个当前未被占用的本地端口，避免与其他集成测试固定端口冲突。
     *
     * @return 可用于启动测试服务端的端口
     * @throws IOException 读取本地端口失败时抛出
     */
    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * 构造 CalculatorService.add 的 RPC 请求消息。
     *
     * @param requestId 请求标识
     * @param left 第一个加数
     * @param right 第二个加数
     * @return 完整的 RPC 请求消息
     */
    private RpcMessage requestMessage(long requestId, int left, int right) {
        RpcRequest request = new RpcRequest(CalculatorService.class.getName(), "add",
                new Object[]{left, right}, new Class<?>[]{int.class, int.class});
        return new RpcMessage(RpcMessage.VERSION_1, RpcMessage.SERIALIZER_JDK,
                RpcMessage.MESSAGE_REQUEST, requestId, RpcMessage.STATUS_SUCCESS, 0, request);
    }

    /**
     * 为此前未发送数据的慢连接补发完整请求，并读取对应响应。
     *
     * @param socket 已连接且正在服务端等待读取的客户端 Socket
     * @param message 待发送的 RPC 请求消息
     * @return 服务端响应消息
     * @throws IOException 网络读写失败时抛出
     */
    private RpcMessage completeSlowRequest(Socket socket, RpcMessage message) throws IOException {
        RpcEncoder encoder = new RpcEncoder();
        RpcDecoder decoder = new RpcDecoder();
        socket.getOutputStream().write(encoder.encode(message));
        socket.getOutputStream().flush();
        return decoder.decode(new DataInputStream(socket.getInputStream()));
    }

    /**
     * 校验响应与请求编号匹配，且加法计算结果正确。
     *
     * @param message 服务端响应消息
     * @param requestId 预期请求标识
     * @param expectedValue 预期加法结果
     */
    private void assertResponse(RpcMessage message, long requestId, int expectedValue) {
        assertEquals(requestId, message.getRequestId());
        RpcResponse response = assertInstanceOf(RpcResponse.class, message.getBody());
        assertEquals(RpcResponse.SUCCESS, response.getStatus());
        assertEquals(expectedValue, response.getReturnValue());
    }
}
