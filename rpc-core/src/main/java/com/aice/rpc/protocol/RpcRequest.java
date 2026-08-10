package com.aice.rpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author aice Cheng
 * Created on 2026/8/9 15:42
 *
 * RPC 协议请求体
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest {
    /**
     * 请求的接口名
     */
    private String interfaceName;

    /**
     * 请求的方法名
     */
    private String methodName;

    /**
     * 参数列表
     */
    private Object[] parameterValues;

    /**
     * 参数类型列表
     */
    private Class<?>[] parameterTypes;
}
