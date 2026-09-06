package com.aice.rpc.protocol;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 RPC 自定义协议编码和解码逻辑。
 *
 * @author aice Cheng
 */
class RpcCodecTest {
    private final RpcEncoder encoder = new RpcEncoder();
    private final RpcDecoder decoder = new RpcDecoder();

    /**
     * 验证请求消息经过编码和解码后，协议头字段和请求体保持不变。
     */
    @Test
    void shouldEncodeAndDecodeRequestMessage() throws IOException {
        RpcRequest request = new RpcRequest(
                "com.aice.rpc.example.GreetingService",
                "sayHello",
                new Object[]{"aice"},
                new Class<?>[]{String.class}
        );
        RpcMessage message = new RpcMessage(
                RpcMessage.VERSION_1,
                RpcMessage.SERIALIZER_JDK,
                RpcMessage.MESSAGE_REQUEST,
                1L,
                RpcMessage.STATUS_SUCCESS,
                0,
                request
        );

        // 编码后再解码，模拟一次完整协议转换。
        byte[] bytes = encoder.encode(message);
        RpcMessage restoredMessage = decoder.decode(bytes);

        assertEquals(RpcMessage.HEADER_LENGTH + message.getBodyLength(), bytes.length);
        assertEquals(RpcMessage.VERSION_1, restoredMessage.getVersion());
        assertEquals(RpcMessage.SERIALIZER_JDK, restoredMessage.getSerializerType());
        assertEquals(RpcMessage.MESSAGE_REQUEST, restoredMessage.getMessageType());
        assertEquals(1L, restoredMessage.getRequestId());
        assertEquals(RpcMessage.STATUS_SUCCESS, restoredMessage.getStatus());
        assertTrue(restoredMessage.getBodyLength() > 0);

        RpcRequest restoredRequest = assertInstanceOf(RpcRequest.class, restoredMessage.getBody());
        assertEquals("com.aice.rpc.example.GreetingService", restoredRequest.getInterfaceName());
        assertEquals("sayHello", restoredRequest.getMethodName());
        assertArrayEquals(new Object[]{"aice"}, restoredRequest.getParameterValues());
        assertArrayEquals(new Class<?>[]{String.class}, restoredRequest.getParameterTypes());
    }

    /**
     * 验证响应消息经过编码和解码后，协议头字段和响应体保持不变。
     */
    @Test
    void shouldEncodeAndDecodeResponseMessage() throws IOException {
        RpcResponse response = new RpcResponse(RpcResponse.SUCCESS, "Hello, aice", null);
        RpcMessage message = new RpcMessage(
                RpcMessage.VERSION_1,
                RpcMessage.SERIALIZER_JDK,
                RpcMessage.MESSAGE_RESPONSE,
                2L,
                RpcMessage.STATUS_SUCCESS,
                0,
                response
        );

        // 编码后再解码，验证响应消息体能够正确还原。
        byte[] bytes = encoder.encode(message);
        RpcMessage restoredMessage = decoder.decode(bytes);

        assertEquals(RpcMessage.MESSAGE_RESPONSE, restoredMessage.getMessageType());
        assertEquals(2L, restoredMessage.getRequestId());
        assertEquals(message.getBodyLength(), restoredMessage.getBodyLength());

        RpcResponse restoredResponse = assertInstanceOf(RpcResponse.class, restoredMessage.getBody());
        assertEquals(RpcResponse.SUCCESS, restoredResponse.getStatus());
        assertEquals("Hello, aice", restoredResponse.getReturnValue());
        assertNull(restoredResponse.getErrorMessage());
    }

    /**
     * 验证魔数错误时，解码器会拒绝该消息。
     */
    @Test
    void shouldRejectInvalidMagic() throws IOException {
        RpcResponse response = new RpcResponse(RpcResponse.SUCCESS, "Hello, aice", null);
        RpcMessage message = new RpcMessage(
                RpcMessage.VERSION_1,
                RpcMessage.SERIALIZER_JDK,
                RpcMessage.MESSAGE_RESPONSE,
                3L,
                RpcMessage.STATUS_SUCCESS,
                0,
                response
        );

        // 修改第一个字节，让协议魔数变成错误值。
        byte[] bytes = encoder.encode(message);
        bytes[0] = 0;

        assertThrows(IllegalStateException.class, () -> decoder.decode(bytes));
    }
}
