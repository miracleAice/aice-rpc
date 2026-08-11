package com.aice.rpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author aice Cheng
 * Created on 2026/8/9 15:42
 * RPC 协议请求体
 *
 * 为实现 JDK 序列化，需实现 java.io.Serializable 接口
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {

    /**
     * JDK 序列化版本号，反序列化时用于判断“字节数据中的类”和“当前代码中的类”版本是否兼容
     */
    @Serial
    private static final long serialVersionUID = 1L;

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
