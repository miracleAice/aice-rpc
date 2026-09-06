package com.aice.rpc.server;

import com.aice.rpc.example.service.CalculatorService;
import com.aice.rpc.example.service.impl.CalculatorServiceImpl;
import com.aice.rpc.protocol.RpcDecoder;
import com.aice.rpc.protocol.RpcEncoder;
import com.aice.rpc.protocol.RpcMessage;
import com.aice.rpc.protocol.RpcRequest;
import com.aice.rpc.protocol.RpcResponse;
import com.aice.rpc.registry.ServiceRegistry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 最小 RPC 服务端，负责监听客户端连接、调用本地服务并返回 RPC 响应。
 *
 * @author aice Cheng
 */
public class RpcServer {
    private final int port;
    private final RpcEncoder encoder;
    private final RpcDecoder decoder;

    // volatile 保证其他线程调用 stop 后，服务端线程能够及时看到最新运行状态。
    private volatile boolean running;

    // 保存当前监听 Socket，使 stop 方法能够主动关闭它并解除 accept 的阻塞。
    private ServerSocket serverSocket;

    private final ServiceRegistry serviceRegistry = new ServiceRegistry();

    /**
     * 创建使用 JDK 序列化的 RPC 服务端。
     *
     * @param port 服务端监听端口
     */
    public RpcServer(int port) {
        this.port = port;
        this.encoder = new RpcEncoder();
        this.decoder = new RpcDecoder();
    }

    /**
     * 启动服务端并持续监听客户端连接。
     * 该方法会阻塞当前线程，直到服务端被停止。
     */
    public void start(){
        // 在开始接收请求前注册本地服务，确保请求到达时能够按接口名找到实现对象。
        registerServices();

        // ServerSocket 使用 try-with-resources 管理，确保正常停止或异常退出时都能释放监听端口。
        try (ServerSocket listeningSocket = new ServerSocket(port)) {
            // 保存监听 Socket，使其他线程可以通过 stop 方法主动关闭它。
            serverSocket = listeningSocket;
            // ServerSocket 创建成功后才标记为运行中，避免端口绑定失败时留下错误状态。
            running = true;

            // 第一版采用同步单线程模型，每次接收并处理完一个连接后再等待下一个连接。
            while (running) {
                // accept 会阻塞等待客户端连接；使用局部变量 listeningSocket，避免依赖可能变化的字段。
                Socket clientSocket = listeningSocket.accept();
                handleClient(clientSocket);
            }
        } catch (IOException exception) {
            // stop 会先把 running 设为 false，再关闭 ServerSocket，使 accept 抛出 IOException。
            // 这种情况属于正常停止；服务仍在运行、端口绑定失败或监听 Socket 未关闭时才属于真实故障。
            if (running || serverSocket == null || !serverSocket.isClosed()) {
                throw new IllegalStateException("服务端连接失败", exception);
            }
        }finally {
            // ServerSocket 已由 try-with-resources 关闭，finally 只负责恢复对象的状态，不在这里抛出关闭异常。
            running = false;
            serverSocket = null;
        }
    }

    /**
     * 读取一个客户端请求，执行对应的本地服务方法，并返回调用结果。
     *
     * @param clientSocket 已建立连接的客户端 Socket
     */
    private void handleClient(Socket clientSocket) {
        // 同时管理客户端 Socket 和输入输出流，确保处理成功或失败时都能释放本次连接。
        try (Socket socket = clientSocket;
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
             DataInputStream inputStream = new DataInputStream(socket.getInputStream())) {

            // 从输入流中读取并解码完整请求消息。
            RpcMessage requestMessage = decoder.decode(inputStream);

            // 处理请求前需先校验请求消息体是否为 RpcRequest 类型。
            RpcResponse serverResponse;
            if ((requestMessage.getBody() instanceof RpcRequest)) {
                // Handler 负责查询本地服务、定位目标方法并反射调用，返回成功或失败的 RpcResponse。
                RpcRequestHandler requestHandler = new RpcRequestHandler(serviceRegistry);
                serverResponse = requestHandler.handle((RpcRequest) requestMessage.getBody());
            }else {
                serverResponse = new RpcResponse(RpcResponse.FAILURE, null, "请求体类型错误");
            }
            // 响应沿用请求的 requestId，使客户端能够确定该响应属于哪一次请求。
            RpcMessage serverMessage = new RpcMessage(
                    requestMessage.getVersion(),
                    RpcMessage.SERIALIZER_JDK,
                    RpcMessage.MESSAGE_RESPONSE,
                    requestMessage.getRequestId(),
                    RpcMessage.STATUS_SUCCESS,
                    0,
                    serverResponse
            );

            // 编码完整响应消息，保留响应类型、requestId 和 RpcResponse。
            byte[] serverBytes = encoder.encode(serverMessage);

            // 将编码后的响应协议字节写入网络连接。
            outputStream.write(serverBytes);

            // 处理结束前刷新输出流，确保响应数据已经写入底层网络连接。
            outputStream.flush();
        }catch (IOException exception) {
            // 将底层网络异常转换为服务端处理异常，同时保留原始异常原因。
            throw new IllegalStateException("处理客户端连接失败", exception);
        }
    }

    /**
     * 注册当前服务端对外提供的本地服务。
     * 请求中的接口全限定名会作为查找服务实现对象的依据。
     * */
    private void registerServices() {
        // CalculatorService.class 是全限定名，不是 "CalculatorService"
        // 第二个参数 Object service 对应的是实际可以执行方法的对象，不是 CalculatorServiceImpl.class
        serviceRegistry.register(CalculatorService.class, new CalculatorServiceImpl());
    }

    /**
     * 停止服务端，并解除 accept 方法的阻塞状态。
     */
    public void stop() {
        // 先修改运行状态，让 start 方法知道接下来的 Socket 关闭属于主动停止。
        running = false;
        try {
            // stop 可能在服务未启动或已经停止时被调用，因此关闭前需要检查 Socket 状态。
            if (serverSocket != null && !serverSocket.isClosed()) {
                // 关闭 ServerSocket，使正在阻塞的 accept 立即结束，服务端线程才能退出循环。
                serverSocket.close();
            }
        }catch (IOException exception) {
            // 将底层关闭异常转换为服务端停止异常，同时保留原始异常原因。
            throw new IllegalStateException("服务端连接关闭失败", exception);
        }
    }

}
