package com.aice.rpc.client;

import com.aice.rpc.registry.ServiceInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 客户端连接管理器
 * 根据服务实例找到与该服务实例建立连接的 RpcClient
 *
 * @author aice Cheng
 * Created on  2026/9/25 15:47
 */
public class RpcClientManager {
    private final Map<ServiceInstance, RpcClient> clients;

    public RpcClientManager() {
        clients = new ConcurrentHashMap<>();
    }

    /**
     * 保存一个服务实例与已建立 RPC 客户端连接的对应关系。
     *
     * @param instance 服务发现或负载均衡后选中的服务实例
     * @param rpcClient 已与该服务实例建立连接的 RPC 客户端
     */
    public void setClient(ServiceInstance instance, RpcClient rpcClient) {
        if (instance == null) {
            throw new IllegalArgumentException("服务实例不能为空");
        }
        if (rpcClient == null) {
            throw new IllegalArgumentException("RPC 客户端不能为空");
        }

        clients.put(instance, rpcClient);
    }

    /**
     * 根据服务实例获取已建立连接的 RPC 客户端。
     *
     * @param instance 服务发现或负载均衡后选中的服务实例
     * @return 与该服务实例对应的 RPC 客户端
     */
    public RpcClient getClient(ServiceInstance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("服务实例不能为空");
        }

        RpcClient rpcClient = clients.get(instance);
        if (rpcClient == null) {
            throw new IllegalStateException("服务实例尚未建立 RPC 客户端连接");
        }
        return rpcClient;
    }

    /**
     * 关闭管理器保存的全部 RPC 客户端连接，并清空连接缓存。
     */
    public void close() {
        RuntimeException closeException = null;
        for (RpcClient rpcClient : clients.values()) {
            try {
                rpcClient.close();
            } catch (RuntimeException exception) {
                // 一个连接关闭失败时，仍继续释放其他连接。
                if (closeException == null) {
                    closeException = exception;
                } else {
                    closeException.addSuppressed(exception);
                }
            }
        }
        clients.clear();

        if (closeException != null) {
            throw closeException;
        }
    }
}
