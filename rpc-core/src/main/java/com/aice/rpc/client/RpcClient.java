package com.aice.rpc.client;

import com.aice.rpc.protocol.RpcMessage;
import com.aice.rpc.serialize.JdkSerializer;
import com.aice.rpc.serialize.Serializer;

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
    private final Serializer serializer;

    /**
     * 创建使用 JDK 序列化的 RPC 客户端。
     *
     * @param host 服务端地址
     * @param port 服务端端口
     */
    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.serializer = new JdkSerializer();
    }

    /**
     * 向服务端发送 RPC 请求，并等待服务端返回响应。
     *
     * @param requestMessage 请求消息
    * @return 服务端返回的响应消息
     */
    public RpcMessage send(RpcMessage requestMessage) {
        // 只校验外层消息是否存在。心跳消息的 data 可以为空，因此这里不校验 requestMessage.getData()。
        if (requestMessage == null) {
            throw new IllegalArgumentException("传输内容为空");
        }

        // 每次调用创建一个 Socket，并通过 try-with-resources 保证正常结束或发生异常时都能关闭网络资源。
        try (Socket socket = new Socket(host, port);
             // DataOutputStream 可以按固定的 4 字节格式写入消息长度，也可以继续写入消息内容。
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
             // DataInputStream 可以按照服务端发送时使用的相同格式读取消息长度和消息内容。
             DataInputStream inputStream = new DataInputStream(socket.getInputStream())
        ) {
            // 序列化完整 RpcMessage，确保 messageType、requestId 和 data 都能够传到服务端。
            byte[] clientData = serializer.serialize(requestMessage);

            // TCP 没有消息边界，因此先发送长度，再发送对应数量的消息字节。
            outputStream.writeInt(clientData.length);
            outputStream.write(clientData);

            // 在等待响应前刷新输出流，确保请求数据已经写入底层网络连接。
            outputStream.flush();

            // 读取响应长度，用它确定本次响应消息体应该读取多少字节。
            int responseLength = inputStream.readInt();
            if (responseLength <= 0) {
                throw new IllegalStateException("响应消息长度必须大于0");
            }

            // 复用已读取并校验的 responseLength；再次调用 readInt 会消耗消息体前 4 个字节，导致数据错位。
            byte[] serverData = new byte[responseLength];

            // 一次普通 read 不保证能读满数组，readFully 会持续读取，直到获得完整响应或连接异常结束。
            inputStream.readFully(serverData);

            // 传入 RpcMessage.class，明确要求反序列化器检查并返回 RpcMessage 类型。
            RpcMessage responseMessage;
            responseMessage = serializer.deserialize(serverData, RpcMessage.class);
            return responseMessage;
        } catch (IOException exception) {
            // 将底层网络异常转换为调用方更容易理解的 RPC 客户端异常，同时保留原始异常原因。
            throw new IllegalStateException("连接失败", exception);
        }
    }
}
