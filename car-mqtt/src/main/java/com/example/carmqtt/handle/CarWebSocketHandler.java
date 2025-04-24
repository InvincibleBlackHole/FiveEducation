package com.example.carmqtt.handle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.carmqtt.entity.CarStatus;
import com.example.carmqtt.service.CarCommandService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
   @author plh
*  @小车WebSockethandler
 * @date 2025-04-18
 * @ Version 1.0
*/
@SuppressWarnings("all")
@Component
public class CarWebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentMap<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, CarStatus> carStatusMap = new ConcurrentHashMap<>();

    @Resource
    private CarCommandService carCommandService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
//        String carId = extractCarId(session);
//        clientSessions.put(carId, session);
//        carStatusMap.putIfAbsent(carId, new CarStatus(carId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JSONObject json = JSON.parseObject(message.getPayload());
            String command = json.getString("command");
            String carId = json.getString("carId");
            // 处理控制指令
            carCommandService.handleCommand(carId, command);
            // 更新状态
            updateCarStatus(carId, command);

            // 发送确认
            sendStatusUpdate(carId);
        } catch (Exception e) {
            // 错误处理
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
//        String carId = extractCarId(session);
//        clientSessions.remove(carId);
//        carStatusMap.remove(carId);
    }

//    private String extractCarId(WebSocketSession session) {
//        // 从请求参数中获取小车ID
//        return session.getUri().getQuery().split("=")[1];
//    }

    public void sendStatusUpdate(String carId) {
        WebSocketSession session = clientSessions.get(carId);
        if (session != null && session.isOpen()) {
            try {
                String jsonStatus = JSON.toJSONString(carStatusMap.get(carId));
                session.sendMessage(new TextMessage(jsonStatus));
            } catch (IOException e) {
                // 处理异常
            }
        }
    }

    private void updateCarStatus(String carId, String command) {
        CarStatus status = carStatusMap.get(carId);
        // 根据指令更新状态
        switch (command) {
            case "FORWARD":
                status.setSpeed(Math.min(status.getSpeed() + 0.5, 5.0));
                break;
            case "BACKWARD":
                status.setSpeed(Math.max(status.getSpeed() - 0.5, -5.0));
                break;
            // 其他指令处理...
        }
        status.setLastUpdate(System.currentTimeMillis());
    }
}
