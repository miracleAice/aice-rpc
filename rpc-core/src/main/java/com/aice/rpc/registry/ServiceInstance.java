package com.aice.rpc.registry;

import lombok.Data;

/**
 * 服务实例
 *
 * @author aice Cheng
 * Created on  2026/9/25 14:15
 */
@Data
public class ServiceInstance {
    private String host;
    private int port;
}
