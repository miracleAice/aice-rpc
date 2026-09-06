package com.aice.rpc.protocol;

import com.aice.rpc.serialize.Serializer;
import com.aice.rpc.serialize.SerializerRegistry;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * RPC 消息解码器。
 * 负责将符合自定义协议格式的字节数组解码为 RpcMessage。
 *
 * @author aice Cheng
 */
public class RpcDecoder {

    /**
     * 将字节数组解码为 RPC 消息。
     *
     * @param bytes 待解码的字节数组
     * @return 解码后的 RPC 消息
     */
    public RpcMessage decode(byte[] bytes) {
        // 校验消息长度是否满足协议头要求。
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("RPC 消息为空");
        }
        if (bytes.length < RpcMessage.HEADER_LENGTH) {
            throw new IllegalStateException("RPC 消息头不完整");
        }

        int magic, bodyLength;
        byte version, serializerType, messageType, status;
        long requestId;
        byte[] bodyBytes;
        try (
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)
                ) {
            // 按协议顺序读取并校验请求头。
            magic = dataInputStream.readInt();
            if (magic != RpcMessage.MAGIC) {
                throw new IllegalStateException("错误的 RPC 协议");
            }
            version = dataInputStream.readByte();
            if (version != RpcMessage.VERSION_1) {
                throw new IllegalStateException("错误的 RPC 版本");
            }
            serializerType = dataInputStream.readByte();
            messageType = dataInputStream.readByte();
            requestId = dataInputStream.readLong();
            status = dataInputStream.readByte();
            bodyLength = dataInputStream.readInt();

            // 校验消息体长度是否合法。
            if (bodyLength < 0) {
                throw new IllegalArgumentException("消息体长度不能小于 0");
            }
            if (bytes.length < RpcMessage.HEADER_LENGTH + bodyLength) {
                throw new IllegalArgumentException("RPC 消息体不完整");
            }
            if (messageType != RpcMessage.MESSAGE_HEART && bodyLength == 0) {
                throw new IllegalArgumentException("请求或响应消息体不能为空");
            }
            bodyBytes = dataInputStream.readNBytes(bodyLength);
        } catch (IOException e) {
            throw new RuntimeException("读取 RPC 消息头消息异常", e);
        }

        // 根据序列化类型选择反序列化器。
        Serializer serializer = SerializerRegistry.getSerializer(serializerType);

        // 根据消息类型反序列化消息体。
        Object body;
        try {
            if (messageType == RpcMessage.MESSAGE_REQUEST) {
                body = serializer.deserialize(bodyBytes, RpcRequest.class);
            } else if (messageType == RpcMessage.MESSAGE_RESPONSE) {
                body = serializer.deserialize(bodyBytes, RpcResponse.class);
            } else if (messageType == RpcMessage.MESSAGE_HEART) {
                body = null;
            } else {
                throw new IllegalArgumentException("RPC 消息类型错误：" + messageType);
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("RPC 消息体反序列化失败", e);
        }

        // 将请求头字段和消息体封装为 RpcMessage。
        RpcMessage rpcMessage = new RpcMessage(
                version,
                serializerType,
                messageType,
                requestId,
                status,
                bodyLength,
                body
        );
        return rpcMessage;
    }
}
