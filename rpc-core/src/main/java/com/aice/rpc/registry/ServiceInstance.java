package com.aice.rpc.registry;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 服务实例
 *
 * @author aice Cheng
 * Created on  2026/9/25 14:15
 */
@Getter
@EqualsAndHashCode
public final class ServiceInstance {
    private final String host;
    private final int port;

    /**
     * 创建不可变的服务实例。
     *
     * @param host 服务地址
     * @param port 服务端口
     */
    public ServiceInstance(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("服务地址不能为空");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("服务端口不合法");
        }

        this.host = host;
        this.port = port;
    }
}
