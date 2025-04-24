package com.example.carmqtt.service.impl;

import com.example.carmqtt.service.CarCommandService;
import org.springframework.stereotype.Service;

/**
   @author plh
*  @小车服务层实现类
 * @date 2025-04-18
 * @ Version 1.0
*/

@Service
public class CarCommandServiceImpl implements CarCommandService {
    @Override
    public void handleCommand(String carId, String command) {
    }
}
