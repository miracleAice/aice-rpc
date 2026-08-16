package com.aice.rpc.registry;

import java.util.HashMap;
import java.util.Map;

/**
 * 本地服务注册表，负责保存服务接口与服务实现对象的对应关系。
 *
 * @author aice Cheng
 */
public class ServiceRegistry {
    private final Map<String, Object> services = new HashMap<>();

    /**
     * 注册一个服务实现对象。
     *
     * @param serviceInterface 服务接口类型
     * @param service 服务实现对象
     */
    public void register(Class<?> serviceInterface, Object service) {
        // 注册前先校验实现对象确实可以作为该接口使用，避免后续反射调用到错误的服务。
        if (!serviceInterface.isInstance(service)) {
            throw new IllegalStateException("服务实现类并未实现指定接口");
        }

        // 使用接口全限定名作为唯一键，使请求中的 interfaceName 能直接定位服务。
        services.put(serviceInterface.getName(), service);
    }

    /**
     * 根据服务接口名获取服务实现对象。
     *
     * @param interfaceName 服务接口全限定名
     * @return 服务实现对象
     */
    public Object getService(String interfaceName) {
        // 先明确判断服务是否存在，避免 Map.get 返回 null 后在后续反射调用时产生不直观的空指针异常。
        if (!services.containsKey(interfaceName)) {
            throw new IllegalStateException("服务不存在");
        }

        // 通过接口全限定名，取出对应的实现对象。
        return services.get(interfaceName);
    }
}
