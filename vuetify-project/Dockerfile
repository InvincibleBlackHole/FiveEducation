# 构建阶段（使用 Node.js 22）
FROM node:22 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
# 复制构建好的前端静态文件
COPY dist/ /usr/share/nginx/html
# 覆盖默认 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
# 声明容器监听端口（可选，实际端口由 docker-compose 映射）
EXPOSE 9080
