<template>
  <v-container class="container-wrapper" fluid>
    <!-- 地图显示区域 -->
    <v-row class="map-container" style="height: 60vh;">
      <v-col cols="12">
        <div id="map-container" class="amap-container" />
      </v-col>
    </v-row>
    <!-- 控制面板区域 -->
    <v-row class="control-panel" style="height: 40vh;">
      <v-col cols="12">
        <v-row>
          <v-col cols="4"> <!-- 左边占比 40% -->
            <v-card class="enhanced-control-card">
                <!--小车照片放置区域-->
            </v-card>
          </v-col>
          <v-col cols="8"> <!-- 右边占比 60% -->
            <v-card class="enhanced-control-card">
              <!-- 车辆选择及状态 -->
              <div class="vehicle-selector">
                <v-chip-group v-model="selectedCarId" mandatory>
                  <v-chip
                    v-for="car in carList"
                    :key="car.id"
                    class="ma-2"
                    :color="getCarColor(car.status)"
                    filter
                    :value="car.id"
                  >
                    <v-icon class="mr-2" :icon="car.icon" />{{ car.name }}
                    <v-icon right>{{ getStatusIcon(car.status) }}</v-icon>
                  </v-chip>
                </v-chip-group>

                <div class="selected-car-info">
                  <v-icon class="mr-2" color="blue" :icon="selectedCarInfo?.icon" large />
                  <span class="car-id ml-2">{{ selectedCarInfo?.id || '未选择车辆' }}</span>
                  <v-chip
                    class="ml-2"
                    :color="getCarColor((selectedCarInfo?.status || 'offline') as CarStatus)"
                    small
                    text-color="white"
                  >
                    电量: {{ selectedCarInfo?.battery || 0 }}% | 状态: {{ selectedCarInfo?.status || '未选择车辆' }}
                  </v-chip>
                </div>
              </div>

              <!-- 控制按钮 -->
              <v-card-actions class="enhanced-control-grid">
                <div class="main-controls">
                  <div class="directional-pad">
                    <div class="cross-controls">
                      <v-btn
                        class="control-btn forward"
                        @mousedown="sendCommand('FORWARD')"
                        @mouseup="sendCommand('STOP')"
                        @touchend.passive="sendCommand('STOP')"
                        @touchstart.passive="sendCommand('FORWARD')"
                      >
                        <v-icon x-large>mdi-arrow-up-thick</v-icon>
                      </v-btn>
                      <div class="horizontal-cross">
                        <v-btn
                          class="control-btn left"
                          @click="sendCommand('LEFT')"
                        >
                          <v-icon x-large>mdi-arrow-left-thick</v-icon>
                        </v-btn>
                        <v-btn
                          class="control-btn stop"
                          @click="sendCommand('STOP')"
                        >
                          <v-icon x-large>mdi-stop-circle</v-icon>
                        </v-btn>
                        <v-btn
                          class="control-btn right"
                          @click="sendCommand('RIGHT')"
                        >
                          <v-icon x-large>mdi-arrow-right-thick</v-icon>
                        </v-btn>
                      </div>
                      <v-btn
                        class="control-btn backward"
                        @mousedown="sendCommand('BACKWARD')"
                        @mouseup="sendCommand('STOP')"
                        @touchend.passive="sendCommand('STOP')"
                        @touchstart.passive="sendCommand('BACKWARD')"
                      >
                        <v-icon x-large>mdi-arrow-down-thick</v-icon>
                      </v-btn>
                    </div>
                  </div>

                  <div class="secondary-controls">
                    <v-btn
                      class="action-btn turn-around"
                      @click="sendCommand('TURN_AROUND')"
                    >
                      <v-icon left>mdi-replay</v-icon> 紧急掉头
                    </v-btn>
                    <div class="speed-controls">
                      <v-btn
                        class="speed-btn speed-up"
                        @click="sendCommand('SPEED_UP')"
                      >
                        <v-icon left>mdi-chevron-up</v-icon> 增速
                      </v-btn>
                      <v-btn
                        class="speed-btn slow-down"
                        @click="sendCommand('SLOW_DOWN')"
                      >
                        <v-icon left>mdi-chevron-down</v-icon> 减速
                      </v-btn>
                    </div>
                  </div>
                </div>
              </v-card-actions>
            </v-card>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
  import { computed, onMounted, onUnmounted, ref } from 'vue';
  import AMapLoader from '@amap/amap-jsapi-loader';

  type Command =
    | 'FORWARD'
    | 'BACKWARD'
    | 'LEFT'
    | 'RIGHT'
    | 'STOP'
    | 'TURN_AROUND'
    | 'SPEED_UP'
    | 'SLOW_DOWN';

  type CarStatus = 'online' | 'offline' | 'busy' | 'error';

  interface CarInfo {
    id: string;
    name: string;
    icon: string;
    position: [number, number];
    status: CarStatus;
    battery: number;
    speed: number;
  }

  // 地图实例
  const map = ref<any>(null);
  // 小车标记集合
  const markers = ref<{ [key: string]: any }>({});
  // WebSocket实例
  const ws = ref<WebSocket | null>(null);
  // 小车列表
  const carList = ref<CarInfo[]>([
    {
      id: 'car-001',
      name: '巡检车01',
      icon: 'mdi-car',
      position: [116.397428, 39.90923],
      status: 'online',
      battery: 85,
      speed: 0,
    },
    {
      id: 'car-002',
      name: '运输车02',
      icon: 'mdi-truck',
      position: [116.407526, 39.90403],
      status: 'busy',
      battery: 50,
      speed: 1.5,
    },
    {
      id: 'car-003',
      name: '运输车03',
      icon: 'mdi-truck',
      position: [115.407526, 39.90403],
      status: 'busy',
      battery: 60,
      speed: 1.5,
    },
  ]);
  // 选中小车ID
  const selectedCarId = ref<string>('car-001');
  // 最后发送的指令
  const lastCommand = ref<Command | null>(null);
  // 最大速度限制
  const MAX_SPEED = 5;

  // 选中小车信息
  const selectedCarInfo = computed(() =>
    carList.value.find(car => car.id === selectedCarId.value) || {
      id: '未选择车辆',
      name: '未选择车辆',
      icon: '',
      position: [0, 0],
      status: 'offline',
      battery: 0,
      speed: 0,
    }
  );

  // 连接状态
  const connected = ref(false);

  // 初始化地图
  const initMap = async () => {
    try {
      const AMap = await AMapLoader.load({
        key: '1a9cc238e5ed39ba8d53470c6efc98bd', // 替换为实际高德API Key
        version: '2.0',
        plugins: ['AMap.Marker', 'AMap.InfoWindow'],
      });

      map.value = new AMap.Map('map-container', {
        zoom: 13,
        center: [116.397428, 39.90923],
      });

      // 初始化小车标记
      carList.value.forEach(car => {
        const marker = new AMap.Marker({
          position: car.position,
          content: createMarkerContent(car),
          offset: new AMap.Pixel(-15, -15),
        });
        marker.on('click', () => handleMarkerClick(car.id));
        markers.value[car.id] = marker;
        map.value.add(marker);
      });
    } catch (error) {
      console.error('地图初始化失败:', error);
    }
  };

  // 创建标记内容
  const createMarkerContent = (car: CarInfo) => {
    const isSelected = car.id === selectedCarId.value;
    return `
    <div class="custom-marker ${isSelected ? 'selected' : ''} ${car.status}">
      <v-icon>${car.icon}</v-icon>
      <div class="pulse"></div>
    </div>
  `;
  };

  // 处理标记点击
  const handleMarkerClick = (carId: string) => {
    selectedCarId.value = carId;
    updateMarkers();
    const car = carList.value.find(c => c.id === carId);
    if (car) {
      map.value.setCenter(car.position);
    }
  };

  // 更新所有标记
  const updateMarkers = () => {
    carList.value.forEach(car => {
      const marker = markers.value[car.id];
      if (marker) {
        marker.setContent(createMarkerContent(car));
      }
    });
  };

  // WebSocket连接
  const connectWebSocket = () => {
    ws.value = new WebSocket('ws://your-backend-url/control');

    ws.value.onopen = () => {
      connected.value = true;
      console.log('WebSocket连接成功');
    };

    ws.value.onmessage = event => {
      const data = JSON.parse(event.data);
      if (data.type === 'STATUS_UPDATE') {
        handleStatusUpdate(data);
      }
    };

    ws.value.onclose = () => {
      connected.value = false;
      setTimeout(connectWebSocket, 3000);
    };
  };

  // 处理状态更新
  const handleStatusUpdate = (data: any) => {
    const index = carList.value.findIndex(c => c.id === data.carId);
    if (index !== -1) {
      carList.value[index] = {
        ...carList.value[index],
        position: data.position,
        battery: data.battery,
        speed: Math.max(-MAX_SPEED, Math.min(MAX_SPEED, data.speed)),
        status: data.status,
      };

      // 更新地图标记位置
      const marker = markers.value[data.carId];
      if (marker) {
        marker.setPosition(data.position);
      }
    }
  };

  // 发送控制指令
  const sendCommand = (command: Command) => {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) return;

    lastCommand.value = command;
    ws.value.send(
      JSON.stringify({
        carId: selectedCarId.value,
        command,
        timestamp: Date.now(),
      })
    );
  };

  // 状态图标
  const getStatusIcon = (status: CarStatus) => {
    const icons = {
      online: 'mdi-check-circle',
      offline: 'mdi-alert-circle',
      busy: 'mdi-clock',
      error: 'mdi-alert',
    };
    return icons[status];
  };
  // 状态颜色
  const getCarColor = (status: CarStatus) => {
    const colors = {
      online: 'success',
      offline: 'grey',
      busy: 'warning',
      error: 'error',
    };
    return colors[status];
  };

  onMounted(() => {
    initMap();
    connectWebSocket();
  });

  onUnmounted(() => {
    if (map.value) {
      map.value.destroy();
    }
    if (ws.value) {
      ws.value.close();
    }
  });
</script>

<style scoped>
:root {
  --primary-blue: #2196F3;
  --success-green: #4CAF50;
  --warning-orange: #FF9800;
  --error-red: #F44336;
  --offline-grey: #9E9E9E;
  --background-gradient: linear-gradient(145deg, #f8f9fa 0%, #e9ecef 100%);
}
.container-wrapper {
  background: var(--background-gradient);
}

.map-container {
  border-radius: 16px;
  margin: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.control-panel {
  flex: 0 0 40vh;
  display: flex;
  align-items: stretch; /* 让子列高度一致 */
}

.amap-container {
  width: 100%;
  height: 100%;
}

.enhanced-control-card {
  background: linear-gradient(145deg, #ffffff 0%, #f8f9fa 100%);
  border-radius: 16px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.vehicle-selector {
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.selected-car-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(245, 245, 245, 0.8);
  border-radius: 24px;
  margin-top: 16px;
}

.custom-marker {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 24px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.control-btn {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.1s ease;
}

.control-btn:active {
  transform: scale(0.95);
}

.control-btn.forward {
  background: #4CAF50;
}

.control-btn.backward {
  background: #FF5252;
}

.control-btn.left,
.control-btn.right {
  background: #2196F3;
}

.control-btn.stop {
  background: #F44336;
  box-shadow: 0 0 16px rgba(244, 67, 54, 0.3);
}

.action-btn,
.speed-btn {
  border-radius: 24px;
  font-weight: 500;
  padding: 12px 24px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.custom-marker.online {
  background-color: #4CAF50;
}
.custom-marker.busy {
  background-color: #FFC107;
}
.custom-marker.offline {
  background-color: #9E9E9E;
}
.custom-marker.error {
  background-color: #F44336;
}

@media (max-width: 768px) {
  .map-container {
    height: 50vh;
  }
  .control-panel {
    height: 50vh;
    flex-direction: column;
  }
  .directional-pad .control-btn {
    width: 60px;
    height: 60px;
  }
  .v-col {
    width: 100%!important;
  }
}

.cross-controls {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
  gap: 16px;
  justify-items: center;
  align-items: center;
}

.cross-controls .control-btn.forward {
  grid-column: 2 / 3;
  grid-row: 1 / 2;
}

.cross-controls .horizontal-cross {
  grid-column: 1 / 4;
  grid-row: 2 / 3;
  display: flex;
  justify-content: center;
  gap: 24px;
}

.cross-controls .control-btn.backward {
  grid-column: 2 / 3;
  grid-row: 3 / 4;
}
.v-chip {
  transition: all 0.2s ease;
  &.online {
    background: linear-gradient(145deg, var(--success-green) 0%, #45a049 100%);
  }
  &.busy {
    background: linear-gradient(145deg, var(--warning-orange) 0%, #FB8C00 100%);
  }
  &.offline {
    background: linear-gradient(145deg, var(--offline-grey) 0%, #757575 100%);
  }
  &.error {
    background: linear-gradient(145deg, var(--error-red) 0%, #d32f2f 100%);
  }
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  }
}

.selected-car-info {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.control-btn {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.1),
    inset 0 -2px 4px rgba(0, 0, 0, 0.05);

  &.forward {
    background: linear-gradient(145deg, #4CAF50 0%, #45a049 100%);
    &:active { background: linear-gradient(145deg, #45a049 0%, #3d8b40 100%); }
  }
  &.backward {
    background: linear-gradient(145deg, #ef5350 0%, #d32f2f 100%);
    &:active { background: linear-gradient(145deg, #d32f2f 0%, #b71c1c 100%); }
  }
  &.left, &.right {
    background: linear-gradient(145deg, #42a5f5 0%, #1976d2 100%);
    &:active { background: linear-gradient(145deg, #1976d2 0%, #1565c0 100%); }
  }
  &.stop {
    background: linear-gradient(145deg, #ef5350 0%, #d32f2f 100%);
    box-shadow: 0 0 16px rgba(244, 67, 54, 0.3);
  }

  &:hover {
    filter: brightness(1.05);
    transform: translateY(-1px);
  }
  &:active {
    filter: brightness(0.95);
    transform: translateY(1px);
  }
}

.custom-marker {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  &::after {
    content: '';
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    animation: pulse 1.5s infinite;
  }

  &.online {
    background: var(--success-green);
    &::after { box-shadow: 0 0 0 rgba(76, 175, 80, 0.4); }
  }
  &.busy {
    background: var(--warning-orange);
    &::after { box-shadow: 0 0 0 rgba(255, 152, 0, 0.4); }
  }
  &.error {
    background: var(--error-red);
    &::after { box-shadow: 0 0 0 rgba(244, 67, 54, 0.4); }
  }
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(76, 175, 80, 0.4); }
  70% { box-shadow: 0 0 0 12px rgba(76, 175, 80, 0); }
  100% { box-shadow: 0 0 0 0 rgba(76, 175, 80, 0); }
}

.action-btn, .speed-btn {
  background: linear-gradient(145deg, #ffffff 0%, #f8f9fa 100%);
  border: 1px solid rgba(0, 0, 0, 0.05);
  &:hover {
    background: linear-gradient(145deg, #f8f9fa 0%, #e9ecef 100%);
  }
}

@media (max-width: 768px) {
  .control-btn {
    width: 64px !important;
    height: 64px !important;
    .v-icon { font-size: 32px !important; }
  }

  .selected-car-info {
    padding: 12px;
    .car-id { font-size: 0.9em; }
  }
}
</style>
