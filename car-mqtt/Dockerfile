# 构建阶段（使用 Maven + JDK 8）
FROM maven:3.8.6-jdk-8 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 运行阶段（使用 JDK 8）
FROM openjdk:8-jdk-slim
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
