package com.example.carmqtt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;

/**
   @author plh
*  @状态广播配置类
 * @date 2025-04-18
 * @ Version 1.0
*/
@Configuration
@EnableScheduling
public class StatusBroadcaster {

//   @Resource
//    private SimpMessagingTemplate messagingTemplate;

//    @Resource
//    private CarStatusRepository statusRepository;
//
//    @Scheduled(fixedRate = 1000)
//    public void broadcastStatus() {
//        statusRepository.getAllStatus().forEach((carId, status) -> {
//            messagingTemplate.convertAndSend("/topic/status/" + carId, status);
//        });
//    }
}
