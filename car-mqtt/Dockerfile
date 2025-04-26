# 构建阶段（使用 Maven + JDK 8）
FROM maven:3.8.6-jdk-8 AS build

# 创建并切换到工作目录
WORKDIR /app

# 复制 pom.xml 和源代码到容器的工作目录
COPY pom.xml ./
COPY src ./src

# 构建项目
RUN mvn clean package -DskipTests

# 运行阶段（使用 JDK 8）
FROM openjdk:8-jdk-slim
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
