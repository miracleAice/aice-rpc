package com.aice.rpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author aice Cheng
 * Created on 2026/8/11 00:14
 *
 * RPC 协议响应体
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse {
    public static final byte SUCCESS = 0;
    public static final byte FAILURE = 1;

    /**
     * 本次响应状态
     */
    private byte status;

    /**
     * 返回值
     */
    private Object returnValue;

    /**
     * 错误信息，调用失败时返回失败原因
     */
    private String errorMessage;
}
