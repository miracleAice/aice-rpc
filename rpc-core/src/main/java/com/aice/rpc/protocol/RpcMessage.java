package com.aice.rpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author aice Cheng
 * Created on 2026/8/9 15:46
 *
 * RPC 协议报文内容
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpcMessage {
    /**
     * 消息类型：1=请求、2=响应、3=心跳
     */
    private byte messageType;

    /**
     * 请求 ID，用于异步响应时关联对应请求
     */
    private String requestId;

    /**
     * 消息体
     */
    private Object data;
}
