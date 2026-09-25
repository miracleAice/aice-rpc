package com.aice.rpc.retry;

import com.aice.rpc.exception.RpcConnectionException;
import com.aice.rpc.exception.RpcException;

/**
 * aice 的第一个重试策略
 *
 * @author aice Cheng
 * Created on  2026/9/25 18:03
 */
public class AiceFirstRetryPolicy implements RetryPolicy {
    @Override
    public boolean shouldRetry(RpcException exception){
        // 初版只允许连接错误进行重试
        return exception instanceof RpcConnectionException;
    }

    @Override
    public int getMaxRetryTimes() {
        return 1;
    }
}
