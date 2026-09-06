package com.aice.rpc.serialize;

import com.aice.rpc.protocol.RpcMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * 序列化器注册中心。
 * 根据序列化类型获取对应的序列化器。
 *
 * @author aice Cheng
 */
public class SerializerRegistry {
    private static final Map<Byte, Serializer> SERIALIZER_MAP = new HashMap<>();

    static {
        SERIALIZER_MAP.put(RpcMessage.SERIALIZER_JDK, new JdkSerializer());
    }

    /**
     * 根据序列化类型获取序列化器。
     *
     * @param serializerType 序列化类型
     * @return 序列化器
     */
    public static Serializer getSerializer(byte serializerType) {
        Serializer serializer = SERIALIZER_MAP.get(serializerType);
        if (serializer == null) {
            throw new IllegalArgumentException("不支持的序列化器类型：" + serializerType);
        }
        return serializer;
    }
}
