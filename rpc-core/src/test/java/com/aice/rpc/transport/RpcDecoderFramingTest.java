package com.aice.rpc.transport;

import com.aice.rpc.protocol.RpcDecoder;
import com.aice.rpc.protocol.RpcEncoder;
import com.aice.rpc.protocol.RpcMessage;
import com.aice.rpc.protocol.RpcResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证流式解码的半包、粘包及异常消息处理。
 */
class RpcDecoderFramingTest {
    private final RpcEncoder encoder = new RpcEncoder();
    private final RpcDecoder decoder = new RpcDecoder();

    /**
     * 模拟同一条消息分段到达，验证消息头和消息体均能跨多次读取完成。
     */
    @Test
    void shouldDecodeFrameReadInChunks() throws IOException {
        byte[] frame = encoder.encode(responseMessage(1L, "chunked response"));
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(frame) {
            /**
             * 每次最多返回三个字节，强制解码器多次读取消息头和消息体。
             */
            @Override
            public synchronized int read(byte[] bytes, int offset, int length) {
                return super.read(bytes, offset, Math.min(length, 3));
            }
        })) {
            assertResponse(decoder.decode(input), 1L, "chunked response");
            assertEquals(-1, input.read());
        }
    }

    /**
     * 拼接两次编码结果，验证每次解码只消费一条消息且保留下一条消息。
     */
    @Test
    void shouldDecodeTwoConcatenatedFrames() throws IOException {
        byte[] first = encoder.encode(responseMessage(2L, "first"));
        byte[] second = encoder.encode(responseMessage(3L, "second, longer response"));
        byte[] combined = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, combined, first.length, second.length);

        try (DataInputStream input = inputOf(combined)) {
            assertResponse(decoder.decode(input), 2L, "first");
            // 字节数组输入流的剩余长度应恰好等于第二条消息的长度。
            assertEquals(second.length, input.available());
            assertResponse(decoder.decode(input), 3L, "second, longer response");
            assertEquals(-1, input.read());
        }
    }

    /**
     * 验证错误魔数、错误版本及消息体未收完整便结束的输入流会被拒绝。
     */
    @Test
    void shouldRejectInvalidMagicVersionAndIncompleteBody() throws IOException {
        byte[] frame = encoder.encode(responseMessage(4L, "invalid frame"));
        byte[] invalidMagic = frame.clone();
        invalidMagic[0] ^= 1;
        try (DataInputStream input = inputOf(invalidMagic)) {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> decoder.decode(input));
            assertEquals("错误的 RPC 协议", error.getMessage());
        }

        byte[] invalidVersion = frame.clone();
        // magic 占四字节，其后的一个字节为 version。
        invalidVersion[Integer.BYTES] = (byte) (RpcMessage.VERSION_1 + 1);
        try (DataInputStream input = inputOf(invalidVersion)) {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> decoder.decode(input));
            assertEquals("错误的 RPC 版本", error.getMessage());
        }

        // 保留原消息头中的 bodyLength，只删除消息体最后一个字节。
        byte[] incompleteBody = Arrays.copyOf(frame, frame.length - 1);
        try (DataInputStream input = inputOf(incompleteBody)) {
            assertThrows(EOFException.class, () -> decoder.decode(input));
        }
    }

    /**
     * 构造带有非空消息体的响应，用不同请求编号和内容区分消息边界。
     */
    private RpcMessage responseMessage(long requestId, String value) {
        return new RpcMessage(RpcMessage.VERSION_1, RpcMessage.SERIALIZER_JDK,
                RpcMessage.MESSAGE_RESPONSE, requestId, RpcMessage.STATUS_SUCCESS, 0,
                new RpcResponse(RpcResponse.SUCCESS, value, null));
    }

    /**
     * 将待测字节包装为解码器要求的数据输入流。
     */
    private DataInputStream inputOf(byte[] bytes) {
        return new DataInputStream(new ByteArrayInputStream(bytes));
    }

    /**
     * 同时校验消息编号、消息类型和响应内容，防止消息错位或串包。
     */
    private void assertResponse(RpcMessage message, long requestId, String value) {
        assertEquals(requestId, message.getRequestId());
        assertEquals(RpcMessage.MESSAGE_RESPONSE, message.getMessageType());
        RpcResponse response = assertInstanceOf(RpcResponse.class, message.getBody());
        assertEquals(RpcResponse.SUCCESS, response.getStatus());
        assertEquals(value, response.getReturnValue());
    }
}
