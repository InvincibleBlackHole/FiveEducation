package com.example.carmqtt.handle;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author plh
 * @小车PC客户端
 * @date 2025-04-16
 * @Version 1.0
 */
@Component
@SuppressWarnings("all")
public class WindowsPCClient {
    private static final String BROKER = "tcp://localhost:1883";
    private static final String CLIENT_ID = "Windows_PC";
    private static MqttClient mqttClient;

    public static void main(String[] args) {
        try {
            // 1. 连接 Broker（注意 Windows 防火墙放行 1883 端口）
            mqttClient = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setKeepAliveInterval(60); // 心跳间隔 60 秒
            mqttClient.connect(options);

            System.out.println("[连接成功] 已连接到 MQTT Broker具体信息为: " + BROKER);

            // 2. 订阅小车消息
            mqttClient.subscribe("car/sensors", (topic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("[小车传感器数据] " + payload);
            });
            System.out.println("[订阅成功] 订阅主题: car/sensors");

            // 3. 订阅 CH340WiFi 模块发布的主题
            mqttClient.subscribe("ch340wifi/data", (topic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("[CH340WiFi 数据] 接收到消息: " + payload);
                // 解析并处理 WiFi 模块返回的数据
                handleWiFiModuleMessage(payload);
            });
            System.out.println("[订阅成功] 订阅主题: ch340wifi/data");

            // 4. 新增订阅：用于接收其他客户端发送的消息
            mqttClient.subscribe("client/communication", (topic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("[接收到客户端消息] " + payload);
            });
            System.out.println("[订阅成功] 订阅主题: client/communication");

            // 5. 获取 WiFi 信息
            getWiFiDetails();

            // 6. 发送控制指令到 CH340WiFi 模块
            publishCommand("forward");

            // 7. 测试：向其他客户端发送消息
            try {
                sendMessageToOtherClient("Hello from WindowsPCClient! for myself !");
            } catch (MqttException e) {
                e.printStackTrace();
            }

            // 保持程序运行
            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫描当前可用的 WiFi 网络
     *
     * @return WiFi 信息字符串
     */
    private static String scanWiFiNetworks() {
        StringBuilder wifiInfo = new StringBuilder();
        try {
            // 调用系统命令扫描 WiFi网络个数
            Process process = Runtime.getRuntime().exec("netsh wlan show networks mode=Bssid");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                wifiInfo.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error scanning WiFi networks: " + e.getMessage();
        }
        return wifiInfo.toString();
    }

    /**
     * 获取 WiFi 模块个数和信息
     */
    private static void getWiFiDetails() {
        String wifiData = scanWiFiNetworks();
        String[] lines = wifiData.split("\n");

        int wifiCount = 0;
        StringBuilder wifiDetails = new StringBuilder();

        for (String line : lines) {
            if (line.contains("SSID")) {
                wifiCount++;
                wifiDetails.append(line.trim()).append("\n");
            }
        }

        System.out.println("[WiFi 模块个数] " + wifiCount);
        System.out.println("[WiFi 信息] " + wifiDetails);

        // 通过 MQTT 发布 WiFi 信息
        try {
            mqttClient.publish("wifi/info", wifiDetails.toString().getBytes(), 0, false);
            System.out.println("[发送 WiFi 信息]");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送控制指令到 CH340WiFi 模块
     *
     * @param command 控制指令
     * @throws MqttException MQTT 异常
     */
    private static void publishCommand(String command) throws MqttException {
        mqttClient.publish("car/control", command.getBytes(), 0, false);
        System.out.println("[发送指令] " + command);
    }

    /**
     * 处理 WiFi 模块返回的消息
     *
     * @param message 消息内容
     */
    private static void handleWiFiModuleMessage(String message) {
        // 示例：解析 JSON 格式的消息
        try {
            // 假设消息是 JSON 格式
            String[] parts = message.split(",");
            for (String part : parts) {
                System.out.println("[解析 WiFi 模块消息] " + part);
            }
        } catch (Exception e) {
            System.out.println("[解析失败] 无法解析 WiFi 模块消息: " + message);
            e.printStackTrace();
        }
    }

    /**
     * 向其他客户端发送消息
     *
     * @param message 要发送的消息内容
     * @throws MqttException MQTT 异常
     */
    private static void sendMessageToOtherClient(String message) throws MqttException {
        mqttClient.publish("client/com0." +
                "" +
                "" +
                "" +
                "" +
                "munication", message.getBytes(), 0, false);
    }
}
