package com.aice.rpc.example.service;

/**
 * 用于验证最小 RPC 调用链路的计算服务接口。
 *
 * @author aice Cheng
 */
public interface CalculatorService {

    /**
     * 计算两个整数之和
     */
    int add(int a, int b);
}
