package com.aice.rpc.exception;

/**
 * RPC 调用过程中发生异常的基础类型。
 *
 * @author aice Cheng
 */
public class RpcException extends RuntimeException {

    /**
     * 创建仅包含错误信息的 RPC 异常。
     *
     * @param message 错误信息
     */
    public RpcException(String message) {
        super(message);
    }

    /**
     * 创建包含错误信息和原始异常原因的 RPC 异常。
     *
     * @param message 错误信息
     * @param cause 原始异常原因
     */
    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
