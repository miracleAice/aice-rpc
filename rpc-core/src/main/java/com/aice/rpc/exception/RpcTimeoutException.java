package com.aice.rpc.exception;

/**
 * 等待 RPC 响应超过调用超时时间时发生的异常。
 *
 * @author aice Cheng
 */
public class RpcTimeoutException extends RpcException {

    /**
     * 创建仅包含错误信息的超时异常。
     *
     * @param message 错误信息
     */
    public RpcTimeoutException(String message) {
        super(message);
    }

    /**
     * 创建包含错误信息和原始异常原因的超时异常。
     *
     * @param message 错误信息
     * @param cause 原始异常原因
     */
    public RpcTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
