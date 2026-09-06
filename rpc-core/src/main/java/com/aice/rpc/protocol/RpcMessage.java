package com.aice.rpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author aice Cheng
 * Created on 2026/8/9 15:46
 * RPC 协议报文内容
 *
 * 为实现 JDK 序列化，需实现 java.io.Serializable 接口
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpcMessage implements Serializable {

    public static final byte MESSAGE_REQUEST = 1; // 请求消息
    public static final byte MESSAGE_RESPONSE = 2; // 响应消息
    public static final byte MESSAGE_HEART = 3; // 心跳消息
    public static final byte VERSION_1 = 1; // 协议版本 1
    public static final byte SERIALIZER_JDK = 1; // JDK 序列化
    public static final byte STATUS_SUCCESS = 0; // 消息正常
    public static final byte STATUS_FAIL = 1; // 消息异常
    public static final int HEADER_LENGTH = 20; // 协议头长度

    /**
     * 魔数，标明是谁的 rpc 框架
     */
    public static final int MAGIC = 0xA1CE08C1;

    /**
     * JDK 序列化版本号，反序列化时用于判断“字节数据中的类”和“当前代码中的类”版本是否兼容
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 协议版本
     */
    private byte version;

    /**
     * 序列化方式
     */
    private byte serializerType;

    /**
     * 消息类型
     */
    private byte messageType;

    /**
     * 请求 ID，用于异步响应时关联对应请求
     */
    private long requestId;

    /**
     * 消息状态
     */
    private byte status;

    /**
     * 消息长度
     */
    private int bodyLength;

    /**
     * 消息体
     */
    private Object body;

}
