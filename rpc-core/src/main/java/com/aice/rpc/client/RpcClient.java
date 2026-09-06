package com.aice.rpc.client;

import com.aice.rpc.protocol.RpcDecoder;
import com.aice.rpc.protocol.RpcEncoder;
import com.aice.rpc.protocol.RpcMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * RPC 客户端，负责向服务端发送请求消息并接收响应消息。
 *
 * @author aice Cheng
 */
public class RpcClient {
    private final String host;
    private final int port;
    private final RpcEncoder encoder;
    private final RpcDecoder decoder;

    /**
     * 创建使用 JDK 序列化的 RPC 客户端。
     *
     * @param host 服务端地址
     * @param port 服务端端口
     */
    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.encoder = new RpcEncoder(); // RPC 协议编码器
        this.decoder = new RpcDecoder(); // RPC 协议解码器
    }

    /**
     * 向服务端发送 RPC 请求，并等待服务端返回响应。
     *
     * @param requestMessage 请求消息
    * @return 服务端返回的响应消息
     */
    public RpcMessage send(RpcMessage requestMessage) {
        // 只校验外层消息是否存在。心跳消息的 body 可以为空，因此这里不校验 requestMessage.getBody()。
        if (requestMessage == null) {
            throw new IllegalArgumentException("传输内容为空");
        }

        // 每次调用创建一个 Socket，并通过 try-with-resources 保证正常结束或发生异常时都能关闭网络资源。
        try (Socket clientSocket = new Socket(host, port);
             // DataOutputStream 负责将编码后的协议字节写入网络连接。
             DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());
             // DataInputStream 负责从网络连接中读取响应头和响应体。
             DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream())
        ) {
            // 将 RpcMessage 编码为自定义协议字节：写入协议头，并序列化消息体
            byte[] clientBytes = encoder.encode(requestMessage);

            // 编码器 encoder 先写入自定义协议的请求头，再写入请求体
            outputStream.write(clientBytes);

            // 在等待响应前刷新输出流，确保请求数据已经写入底层网络连接。
            outputStream.flush();

            // 从输入流中读取并解码完整响应消息。
            return decoder.decode(inputStream);
        } catch (IOException exception) {
            // 将底层网络异常转换为调用方更容易理解的 RPC 客户端异常，同时保留原始异常原因。
            throw new IllegalStateException("客户端连接失败", exception);
        }
    }
}
