package com.aice.rpc.exception;

/**
 * 与服务实例建立连接或使用连接时发生的异常。
 *
 * @author aice Cheng
 */
public class RpcConnectionException extends RpcException {

    /**
     * 创建仅包含错误信息的连接异常。
     *
     * @param message 错误信息
     */
    public RpcConnectionException(String message) {
        super(message);
    }

    /**
     * 创建包含错误信息和原始异常原因的连接异常。
     *
     * @param message 错误信息
     * @param cause 原始异常原因
     */
    public RpcConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
