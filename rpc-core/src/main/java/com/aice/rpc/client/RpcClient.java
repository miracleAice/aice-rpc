package com.aice.rpc.client;

import com.aice.rpc.protocol.RpcDecoder;
import com.aice.rpc.protocol.RpcEncoder;
import com.aice.rpc.protocol.RpcMessage;

import java.io.ByteArrayInputStream;
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
            // 序列化完整 RpcMessage，确保 messageType、requestId 和 body 都能够传到服务端。
            byte[] clientBytes = encoder.encode(requestMessage);

            // 编码器 encoder 先写入自定义协议的请求头，再写入请求体
            outputStream.write(clientBytes);

            // 在等待响应前刷新输出流，确保请求数据已经写入底层网络连接。
            outputStream.flush();

            // 先读取响应头，获取响应体长度。
            byte[] headerBytes = new byte[RpcMessage.HEADER_LENGTH];
            inputStream.readFully(headerBytes);

            int bodyLength = getBodyLength(headerBytes);

            // 根据响应体长度读取响应体。
            byte[] bodyBytes = new byte[bodyLength];
            // 一次普通 read 不保证能读满数组，readFully 会持续读取，直到获得完整响应或连接异常结束。
            inputStream.readFully(bodyBytes);

            // 将响应头和响应体字节数组拼在一起解码，获取返回的 RpcMessage。
            byte[] serverBytes = new byte[RpcMessage.HEADER_LENGTH + bodyLength];
            System.arraycopy(headerBytes, 0, serverBytes, 0, headerBytes.length);
            System.arraycopy(bodyBytes, 0, serverBytes, headerBytes.length, bodyBytes.length);

            // 将完整响应交给解码器解析为 RpcMessage。
            return decoder.decode(serverBytes);
        } catch (IOException exception) {
            // 将底层网络异常转换为调用方更容易理解的 RPC 客户端异常，同时保留原始异常原因。
            throw new IllegalStateException("客户端连接失败", exception);
        }
    }

    /**
     * 从协议头字节数组中读取消息体长度。
     *
     * @param headerBytes 协议头字节数组
     * @return 消息体长度
     * @throws IOException 读取协议头失败时抛出
     */
    private int getBodyLength(byte[] headerBytes) throws IOException {
        int bodyLength;
        try (
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(headerBytes);
                DataInputStream headerInputStream = new DataInputStream(byteArrayInputStream)
                ) {
            headerInputStream.readInt();      // magic
            headerInputStream.readByte();     // version
            headerInputStream.readByte();     // serializerType
            headerInputStream.readByte();     // messageType
            headerInputStream.readLong();     // requestId
            headerInputStream.readByte();     // status
            bodyLength = headerInputStream.readInt();
        }
        return bodyLength;
    }
}
