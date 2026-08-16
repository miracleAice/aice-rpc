package com.aice.rpc.server;

import com.aice.rpc.protocol.RpcRequest;
import com.aice.rpc.protocol.RpcResponse;
import com.aice.rpc.registry.ServiceRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * RPC 请求处理器，负责将 RPC 请求转发给本地服务实现对象。
 *
 * @author aice Cheng
 */
public class RpcRequestHandler {
    private final ServiceRegistry serviceRegistry;

    /**
     * 创建 RPC 请求处理器。
     *
     * @param serviceRegistry 本地服务注册表
     */
    public RpcRequestHandler(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * 处理 RPC 请求并返回调用结果。
     *
     * @param request RPC 请求
     * @return RPC 响应
     */
    public RpcResponse handle(RpcRequest request) {
        try {
            // 请求中保存的是接口全限定名，先通过它从本地注册表找到实际的服务实现对象。
            String interfaceName = request.getInterfaceName();
            Object service = serviceRegistry.getService(interfaceName);

            // 方法名和参数类型共同确定目标方法，参数类型不能省略，否则重载方法无法准确区分。
            Method method = service.getClass().getMethod(request.getMethodName(), request.getParameterTypes());

            // 反射调用返回 Object，以兼容不同服务方法的返回类型。
            Object result = method.invoke(service, request.getParameterValues());

            // 调用成功后，将业务返回值包装为统一的 RPC 成功响应。
            return new RpcResponse(RpcResponse.SUCCESS, result, null);
        // 不同的异常需要返回对应的失败响应，而不是直接中断请求。
        } catch (IllegalStateException exception) {
            return new RpcResponse(RpcResponse.FAILURE, null, "目标服务不存在");
        } catch (NoSuchMethodException exception) {
            return new RpcResponse(RpcResponse.FAILURE, null, "目标方法不存在");
        } catch (InvocationTargetException exception) {
            return new RpcResponse(RpcResponse.FAILURE, null, "目标方法执行时出现异常");
        } catch (IllegalAccessException exception) {
            return new RpcResponse(RpcResponse.FAILURE, null, "无目标方法的访问权限");
        }
    }
}
