package com.aice.rpc.serialize;

import com.aice.rpc.protocol.RpcRequest;
import com.aice.rpc.protocol.RpcResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JdkSerializerTest {
    private final Serializer serializer = new JdkSerializer();

    /**
     * 验证请求体经过序列化和反序列化后，所有字段保持不变。
     */
    @Test
    void shouldSerializeAndDeserializeRequestBody() {
        RpcRequest request = new RpcRequest(
                "com.aice.rpc.example.GreetingService",
                "sayHello",
                new Object[]{"aice"},
                new Class<?>[]{String.class}
        );

        // 自定义协议只把 body 交给序列化器处理。
        byte[] bytes = serializer.serialize(request);
        RpcRequest restoredRequest = serializer.deserialize(bytes, RpcRequest.class);
        assertEquals("com.aice.rpc.example.GreetingService", restoredRequest.getInterfaceName());
        assertEquals("sayHello", restoredRequest.getMethodName());
        assertArrayEquals(new Object[]{"aice"}, restoredRequest.getParameterValues());
        assertArrayEquals(new Class<?>[]{String.class}, restoredRequest.getParameterTypes());
    }

    /**
     * 验证成功响应体经过序列化和反序列化后，状态和返回值保持不变。
     */
    @Test
    void shouldSerializeAndDeserializeSuccessResponseBody() {
        RpcResponse response = new RpcResponse(RpcResponse.SUCCESS, "Hello, aice", null);

        // 自定义协议只把 body 交给序列化器处理。
        byte[] bytes = serializer.serialize(response);
        RpcResponse restoredResponse = serializer.deserialize(bytes, RpcResponse.class);
        assertEquals(RpcResponse.SUCCESS, restoredResponse.getStatus());
        assertEquals("Hello, aice", restoredResponse.getReturnValue());
        assertNull(restoredResponse.getErrorMessage());
    }

    /**
     * 验证失败响应体经过序列化和反序列化后，状态和错误信息保持不变。
     */
    @Test
    void shouldSerializeAndDeserializeFailureResponseBody() {
        RpcResponse response = new RpcResponse(RpcResponse.FAILURE, null, "服务调用失败");

        // 自定义协议只把 body 交给序列化器处理。
        byte[] bytes = serializer.serialize(response);
        RpcResponse restoredResponse = serializer.deserialize(bytes, RpcResponse.class);
        assertEquals(RpcResponse.FAILURE, restoredResponse.getStatus());
        assertNull(restoredResponse.getReturnValue());
        assertEquals("服务调用失败", restoredResponse.getErrorMessage());
    }
}
