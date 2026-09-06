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

    /**
     * JDK 序列化版本号，反序列化时用于判断“字节数据中的类”和“当前代码中的类”版本是否兼容
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型
     */
    private byte messageType;

    /**
     * 请求 ID，用于异步响应时关联对应请求
     */
    private long requestId;

    /**
     * 消息体
     */
    private Object body;
}
