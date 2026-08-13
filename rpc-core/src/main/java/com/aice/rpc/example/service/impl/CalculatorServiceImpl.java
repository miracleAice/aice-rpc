package com.aice.rpc.example.service.impl;

import com.aice.rpc.example.service.CalculatorService;

/**
 * 计算服务的默认实现，用于服务端执行真实方法。
 *
 * @author aice Cheng
 */
public class CalculatorServiceImpl implements CalculatorService {

    /**
     * 计算两个整数之和
     */
    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
