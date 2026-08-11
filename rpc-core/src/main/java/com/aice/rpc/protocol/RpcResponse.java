package com.aice.rpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author aice Cheng
 * Created on 2026/8/11 00:14
 * RPC 协议响应体
 *
 * 为实现 JDK 序列化，需实现 java.io.Serializable 接口
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse implements Serializable {

    /**
     * JDK 序列化版本号，反序列化时用于判断“字节数据中的类”和“当前代码中的类”版本是否兼容
     */
    @Serial
    private static final long serialVersionUID = 1L;

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
