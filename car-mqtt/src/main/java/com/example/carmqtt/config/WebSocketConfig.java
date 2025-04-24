package com.example.carmqtt.config;

import com.example.carmqtt.handle.CarWebSocketHandler;
import jdk.nashorn.internal.ir.annotations.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

/**
   @author plh
*  @webSocket 配置类
 * @date 2025-04-18
 * @ Version 1.0
*/
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private CarWebSocketHandler carWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(carWebSocketHandler, "/control")
                .setAllowedOrigins("*");
    }
}
