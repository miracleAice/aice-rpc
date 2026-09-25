package com.aice.rpc.exception;

/**
 * 服务端已接收请求但执行失败时发生的异常。
 *
 * @author aice Cheng
 */
public class RpcRemoteException extends RpcException {

    /**
     * 创建仅包含错误信息的远程调用异常。
     *
     * @param message 错误信息
     */
    public RpcRemoteException(String message) {
        super(message);
    }

    /**
     * 创建包含错误信息和原始异常原因的远程调用异常。
     *
     * @param message 错误信息
     * @param cause 原始异常原因
     */
    public RpcRemoteException(String message, Throwable cause) {
        super(message, cause);
    }
}
