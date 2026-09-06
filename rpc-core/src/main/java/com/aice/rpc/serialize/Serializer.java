package com.aice.rpc.serialize;

/**
 * @author aice Cheng
 * Created on  2026/8/11 23:53
 */
public interface Serializer {
    /**
     * 序列化
     * */
    byte[] serialize(Object object);

    /**
     * 反序列化
     * */
    <T> T deserialize(byte[] bytes, Class<T> targetClass);
}
