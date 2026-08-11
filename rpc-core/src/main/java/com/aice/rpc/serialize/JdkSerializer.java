package com.aice.rpc.serialize;

import java.io.*;

/**
 * @author aice Cheng
 * Created on  2026/8/12 00:02
 */
public class JdkSerializer implements Serializer{

    /**
     * 使用 JDK 序列化将对象转换为字节数组
     *
     * @param object 待序列化对象
     * @return 序列化后的字节数组
     */
    @Override
    public byte[] serialize(Object object){
        if (object == null) {
            throw new IllegalArgumentException("序列化对象不能为空");
        }
        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
                ) {
            // 将对象写入字节输出流
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();

            return byteArrayOutputStream.toByteArray();
        }catch (IOException exception) {
            throw new IllegalStateException("JDK序列化失败", exception);
        }
    }

    /**
     * 使用 JDK 反序列化将字节数组还原为目标对象
     *
     * @param bytes 待反序列化的字节数组
     * @param targetClass 目标类型
     * @param <T> 目标对象类型
     * @return 反序列化后的对象
     */
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass){
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("反序列化字节数组不能为空");
        }
        try (
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)
                ) {
            // 读取对象并检查类型
            Object object = objectInputStream.readObject();

            return targetClass.cast(object );
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("JDK反序列化失败", exception);
        }
    }
}
