package com.aice.rpc.serialize;

import com.aice.rpc.protocol.RpcMessage;
import com.aice.rpc.protocol.RpcRequest;
import com.aice.rpc.protocol.RpcResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class JdkSerializerTest {
    private final Serializer serializer = new JdkSerializer();

    /**
     * 验证请求消息经过序列化和反序列化后，所有字段保持不变。
     */
    @Test
    void shouldSerializeAndDeserializeRequestMessage() {
        RpcRequest request = new RpcRequest(
                "com.aice.rpc.example.GreetingService",
                "sayHello",
                new Object[]{"aice"},
                new Class<?>[]{String.class}
        );
        RpcMessage message = new RpcMessage(
                RpcMessage.VERSION,
                RpcMessage.SERIALIZER_JDK,
                RpcMessage.MESSAGE_REQUEST,
                1L,
                RpcMessage.STATUS_SUCCESS,
                0,
                request
        );

        // 将完整请求消息转换为字节数组，再还原为 RpcMessage。
        byte[] bytes = serializer.serialize(message);
        RpcMessage restoredMessage = serializer.deserialize(bytes, RpcMessage.class);

        assertEquals(RpcMessage.MESSAGE_REQUEST, restoredMessage.getMessageType());
        assertEquals(1L, restoredMessage.getRequestId());
        assertInstanceOf(RpcRequest.class, restoredMessage.getBody());

        RpcRequest restoredRequest = (RpcRequest) restoredMessage.getBody();
        assertEquals("com.aice.rpc.example.GreetingService", restoredRequest.getInterfaceName());
        assertEquals("sayHello", restoredRequest.getMethodName());
        assertArrayEquals(new Object[]{"aice"}, restoredRequest.getParameterValues());
        assertArrayEquals(new Class<?>[]{String.class}, restoredRequest.getParameterTypes());
    }

    /**
     * 验证成功响应经过序列化和反序列化后，状态和返回值保持不变。
     */
    @Test
    void shouldSerializeAndDeserializeSuccessResponseMessage() {
        RpcResponse response = new RpcResponse(RpcResponse.SUCCESS, "Hello, aice", null);
        RpcMessage message = new RpcMessage(
                RpcMessage.VERSION,
                RpcMessage.SERIALIZER_JDK,
                RpcMessage.MESSAGE_RESPONSE,
                1L,
                RpcMessage.STATUS_SUCCESS,
                0,
                response
        );

        // 对成功响应进行一次完整的序列化往返。
        byte[] bytes = serializer.serialize(message);
        RpcMessage restoredMessage = serializer.deserialize(bytes, RpcMessage.class);

        assertEquals(RpcMessage.MESSAGE_RESPONSE, restoredMessage.getMessageType());
        assertEquals(1L, restoredMessage.getRequestId());
        assertInstanceOf(RpcResponse.class, restoredMessage.getBody());

        RpcResponse restoredResponse = (RpcResponse) restoredMessage.getBody();
        assertEquals(RpcResponse.SUCCESS, restoredResponse.getStatus());
        assertEquals("Hello, aice", restoredResponse.getReturnValue());
        assertNull(restoredResponse.getErrorMessage());
    }

    /**
     * 验证失败响应经过序列化和反序列化后，状态和错误信息保持不变。
     */
    @Test
    void shouldSerializeAndDeserializeFailureResponseMessage() {
        RpcResponse response = new RpcResponse(RpcResponse.FAILURE, null, "服务调用失败");
        RpcMessage message = new RpcMessage(
                RpcMessage.VERSION,
                RpcMessage.SERIALIZER_JDK,
                RpcMessage.MESSAGE_RESPONSE,
                1L,
                RpcMessage.STATUS_SUCCESS,
                0,
                response
        );

        // 对失败响应进行一次完整的序列化往返。
        byte[] bytes = serializer.serialize(message);
        RpcMessage restoredMessage = serializer.deserialize(bytes, RpcMessage.class);

        assertEquals(RpcMessage.MESSAGE_RESPONSE, restoredMessage.getMessageType());
        assertEquals(1L, restoredMessage.getRequestId());
        assertInstanceOf(RpcResponse.class, restoredMessage.getBody());

        RpcResponse restoredResponse = (RpcResponse) restoredMessage.getBody();
        assertEquals(RpcResponse.FAILURE, restoredResponse.getStatus());
        assertNull(restoredResponse.getReturnValue());
        assertEquals("服务调用失败", restoredResponse.getErrorMessage());
    }
}
