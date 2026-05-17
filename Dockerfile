# ========== Stage 1: Build ==========
# Dùng JDK để compile source code thành JAR
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper + pom.xml TRƯỚC
# → Docker cache layer này, chỉ re-download dependencies khi pom.xml thay đổi
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Cấp quyền thực thi cho Maven wrapper
RUN chmod +x mvnw

# Download dependencies (cached nếu pom.xml không đổi)
RUN ./mvnw dependency:go-offline -B

# Copy source code SAU (layer này thay đổi thường xuyên)
COPY src src

# Build JAR, skip tests (tests chạy riêng trong CI)
RUN ./mvnw clean package -DskipTests -B

# ========== Stage 2: Run ==========
# Chỉ dùng JRE (nhẹ hơn JDK) để chạy app
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy JAR từ stage build
COPY --from=build /app/target/*.jar app.jar

# Port mà Spring Boot lắng nghe
EXPOSE 8080

# Chạy app
ENTRYPOINT ["java", "-jar", "app.jar"]
