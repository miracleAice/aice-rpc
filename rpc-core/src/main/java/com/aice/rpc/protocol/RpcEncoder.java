package com.aice.rpc.protocol;

import com.aice.rpc.serialize.Serializer;
import com.aice.rpc.serialize.SerializerRegistry;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * RPC 消息编码器。
 * 负责将 RpcMessage 编码为符合自定义协议格式的字节数组。
 *
 * @author aice Cheng
 */
public class RpcEncoder {
    /**
     * 将 RPC 消息编码为字节数组。
     *
     * @param message RPC 消息
     * @return 编码后的字节数组
     */
    public byte[] encode(RpcMessage message) throws IOException {
        // 校验消息不能为空。
        if (message == null) {
            throw new IllegalArgumentException("Rpc 消息不能为空");
        }

        // 根据消息中的序列化类型选择序列化器。
        Serializer serializer = SerializerRegistry.getSerializer(message.getSerializerType());

        // 将消息体序列化为字节数组。
        byte[] bodyBytes = new byte[0];
        // 兼容心跳消息 body 为空的情况。
        if (message.getBody() != null) {
            bodyBytes = serializer.serialize(message.getBody());
        }

        // 计算消息体长度，并回填到消息对象。
        int bodyLength = bodyBytes.length;
        message.setBodyLength(bodyLength);
        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                ){
            // 按协议顺序写入请求头。
            dataOutputStream.writeInt(RpcMessage.MAGIC);
            dataOutputStream.writeByte(message.getVersion());
            dataOutputStream.writeByte(message.getSerializerType());
            dataOutputStream.writeByte(message.getMessageType());
            dataOutputStream.writeLong(message.getRequestId());
            dataOutputStream.writeByte(message.getStatus());
            dataOutputStream.writeInt(bodyLength);

            // 写入消息体字节数组。
            dataOutputStream.write(bodyBytes);

            // 返回完整协议消息。
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new IOException("协议序列化写入异常", e);
        }
    }
}
