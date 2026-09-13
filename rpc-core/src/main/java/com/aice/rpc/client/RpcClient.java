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

    // 长连接实现：Socket、DataOutputStream 和 DataInputStream 改为成员字段。
    // 它们由整个 RpcClient 实例复用，不能再在 send 方法中使用 try-with-resources 自动关闭。
    private final Socket clientSocket;
    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;

    /**
     * 创建使用 JDK 序列化的 RPC 客户端。
     *
     * @param host 服务端地址
     * @param port 服务端端口
     */
    public RpcClient(String host, int port){
        this.host = host;
        this.port = port;
        this.encoder = new RpcEncoder(); // RPC 协议编码器
        this.decoder = new RpcDecoder(); // RPC 协议解码器
        // 使用局部变量保存尚未完成初始化的 Socket，避免中途失败时泄漏连接。
        Socket socket = null;
        try {
            // 先建立 TCP 连接，再基于同一个连接创建输入输出流。
            socket = new Socket(host, port);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream());

            // 全部资源都初始化成功后，再赋值给不可变成员字段。
            this.clientSocket = socket;
            this.outputStream = output;
            this.inputStream = input;
        } catch (IOException exception) {
            // 初始化过程中失败时，关闭 Socket 会同时关闭已关联的输入输出流。
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException closeException) {
                    // 保留关闭失败原因，便于调用方获取完整的异常信息。
                    exception.addSuppressed(closeException);
                }
            }
            // 将底层网络异常转换为调用方更容易理解的 RPC 客户端异常，同时保留原始异常原因。
            throw new IllegalStateException("客户端连接失败", exception);
        }
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

        // send 只负责编码、写入、刷新和读取本次响应；不要在此处关闭连接。
        try {
            // 将 RpcMessage 编码为自定义协议字节：写入协议头，并序列化消息体
            byte[] clientBytes = encoder.encode(requestMessage);

            // 编码器 encoder 先写入自定义协议的请求头，再写入请求体
            outputStream.write(clientBytes);

            // 在等待响应前刷新输出流，确保请求数据已经写入底层网络连接。
            outputStream.flush();

            // 从输入流中读取并解码完整响应消息。
            return decoder.decode(inputStream);
        }catch (IOException e) {
            throw new RuntimeException("客户端写入失败", e);
        }
    }

    // 调用方完成全部 RPC 调用后显式调用该方法关闭连接。
    public void close() {
        try {
            // 关闭 socket 后输入/输出流也会随之关闭
            this.clientSocket.close();
        }catch (IOException e) {
            throw new RuntimeException("连接关闭失败", e);
        }
    }

}
