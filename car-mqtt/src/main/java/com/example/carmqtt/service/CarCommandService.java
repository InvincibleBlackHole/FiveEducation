package com.example.carmqtt.service;

/**
 * @author plh
 * @小车服务层
 * @date 2025-04-18
 * @ Version 1.0
 */
public interface CarCommandService {
    public void handleCommand(String carId, String command);
}
