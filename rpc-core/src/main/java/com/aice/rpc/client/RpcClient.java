package com.aice.rpc.client;

import com.aice.rpc.protocol.RpcDecoder;
import com.aice.rpc.protocol.RpcEncoder;
import com.aice.rpc.protocol.RpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RPC 客户端，负责向服务端发送请求消息并接收响应消息。
 *
 * @author aice Cheng
 */
public class RpcClient {
    private static final Logger log = LoggerFactory.getLogger(RpcClient.class);
    private static final int TIME_OUT_SECONDS = 66;

    private final String host;
    private final int port;
    private final RpcEncoder encoder;
    private final RpcDecoder decoder;

    // 长连接实现：Socket、DataOutputStream 和 DataInputStream 改为成员字段。
    // 它们由整个 RpcClient 实例复用，不能再在 send 方法中使用 try-with-resources 自动关闭。
    private final Socket clientSocket;
    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;

    // 保存未完成请求，并串行化同一连接的写入。
    private final ConcurrentHashMap<Long, CompletableFuture<RpcMessage>> pendingRequest = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Thread responseThread;
    // 标记连接是否已关闭或响应读取线程是否已终止，阻止后续请求永久等待响应。
    private volatile boolean closed;

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
        // 启动唯一的响应读取线程。
        // 该线程是 inputStream 唯一的读取方，循环解码响应并按 requestId 完成 pendingRequests 中的 Future。
        responseThread = new Thread(() -> {
            while (true) {
                // receiveResponse 返回 false 表示连接已关闭或读取失败，结束响应读取线程。
                if (!receiveResponse(inputStream)) {
                    break;
                }
            }
        });
        responseThread.start();
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

        // 先创建本次请求对应的等待结果。
        long requestId = requestMessage.getRequestId();
        CompletableFuture<RpcMessage> requestFuture = new CompletableFuture<>();

        // 使用写锁协调 send 与 close，避免关闭后仍有请求被放入 pendingRequest。
        lock.lock();
        try {
            if (closed || clientSocket.isClosed()) {
                throw new IllegalStateException("客户端连接已关闭");
            }
            // 先登记再写入，避免快速响应找不到对应 Future。
            if (pendingRequest.putIfAbsent(requestId, requestFuture) != null) {
                throw new IllegalStateException("请求编号已存在：" + requestId);
            }
            // 将 RpcMessage 编码为自定义协议字节：写入协议头，并序列化消息体
            byte[] clientBytes = encoder.encode(requestMessage);
            // 编码器 encoder 先写入自定义协议的请求头，再写入请求体。
            outputStream.write(clientBytes);
            // 在等待响应前刷新输出流，确保请求数据已经写入底层网络连接。
            outputStream.flush();

        } catch (IOException exception) {
            // 写入失败时清理本次等待请求。
            pendingRequest.remove(requestId, requestFuture);
            requestFuture.completeExceptionally(exception);
            throw new RuntimeException("客户端写入失败", exception);
        } catch (RuntimeException exception) {
            // 编码失败或连接状态异常时，清理可能已登记的本次请求。
            pendingRequest.remove(requestId, requestFuture);
            requestFuture.completeExceptionally(exception);
            throw exception;
        } finally {
            // 写入是否成功都要释放锁，其他请求才能继续写入同一连接。
            lock.unlock();
        }

        // 等待响应读取线程完成本次请求。
        try {
            return requestFuture.get(TIME_OUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            // 调用线程被中断时清理本次请求，并恢复中断标记。
            pendingRequest.remove(requestId, requestFuture);
            requestFuture.completeExceptionally(exception);
            // requestFuture.get 抛出 InterruptedException 后会清除中断标记，
            // 此处恢复调用 send 的当前线程标记。让上层代码知道这个线程曾被中断
            Thread.currentThread().interrupt();
            throw new RuntimeException("客户端等待响应时被中断", exception);
        } catch (ExecutionException exception) {
            throw new RuntimeException("客户端处理响应失败", exception.getCause());
        } catch (TimeoutException exception) {
            // 超时后删除等待记录，避免迟迟不返回的请求长期占用内存。
            pendingRequest.remove(requestId, requestFuture);
            throw new RuntimeException("客户端等待请求超时", exception);
        }
    }

    // 调用方完成全部 RPC 调用后显式调用该方法关闭连接。
    public void close() {
        IOException closeException = null;
        // 与 send 共用锁，防止 close 清理完成后仍有新请求登记到 pendingRequest。
        lock.lock();
        try {
            closed = true;
            // 关闭 socket 后输入/输出流也会随之关闭
            this.clientSocket.close();
        } catch (IOException exception) {
            closeException = exception;
        } finally {
            lock.unlock();
            // 中断读取线程并通知所有等待请求。
            this.responseThread.interrupt();
            pendingRequest.values().forEach(future ->
                    future.completeExceptionally(new InterruptedException("连接关闭，进行中的请求被中断")));
            // 已通知所有等待请求后清空 Map，避免已关闭连接保留请求数据。
            pendingRequest.clear();
        }
        if (closeException != null) {
            throw new RuntimeException("连接关闭失败", closeException);
        }
    }

    // 读取一条响应并完成对应请求；返回 false 时结束读取线程。
    private boolean receiveResponse(DataInputStream inputStream) {
        RpcMessage serverMessage;
        try {
            serverMessage = decoder.decode(inputStream);
            long currentRequestId = serverMessage.getRequestId();
            // 原子地取出并删除 Future，防止同一响应被重复完成，也避免重复查询 Map。
            CompletableFuture<RpcMessage> future = pendingRequest.remove(currentRequestId);
            if (future == null) {
                log.warn("{} 请求 ID 不存在", currentRequestId);
                return true;
            }
            future.complete(serverMessage);
            return true;
        } catch (IOException | RuntimeException exception) {
            // 读取失败时终止连接并通知所有等待请求。
            // 先设置关闭状态并持有写锁，确保不会有新请求在清理过程中进入 Map。
            lock.lock();
            try {
                closed = true;
                pendingRequest.values().forEach(future -> future.completeExceptionally(exception));
                pendingRequest.clear();
            } finally {
                lock.unlock();
            }
            // 响应读取线程结束后关闭 Socket，释放本地连接资源。
            boolean socketAlreadyClosed = clientSocket.isClosed();
            try {
                clientSocket.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            // 连接已由 close 主动关闭时不记录错误；其他读取失败记录完整异常。
            if (!socketAlreadyClosed) {
                log.warn("响应读取线程已停止", exception);
            }
            return false;
        }
    }

}
