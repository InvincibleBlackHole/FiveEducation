package com.example.carmqtt.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
   @author plh
*  @小车状态实体类
 * @date 2025-04-18
 * @ Version 1.0
*/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CarStatus {
    private String carId;
    private double speed;
    private double battery = 100.0;
    private String status = "online";
    private long lastUpdate;

    public CarStatus(String carId) {
        this.carId = carId;
    }
}
