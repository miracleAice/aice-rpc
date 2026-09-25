package com.aice.rpc.retry;

import com.aice.rpc.exception.RpcException;

/**
 * RPC 重试策略接口。
 *
 * @author aice Cheng
 */
public interface RetryPolicy {
    
    /**
     * 判断本次 RPC 调用失败后是否允许重试。
     *
     * @param exception 本次调用失败时抛出的 RPC 异常
     * @return 是否允许重试
     */
    boolean shouldRetry(RpcException exception);

    /**
     * 获取一次 RPC 调用允许执行的最大重试次数。
     *
     * @return 最大重试次数
     */
    int getMaxRetryTimes();
}
