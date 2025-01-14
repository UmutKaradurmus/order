FROM eclipse-temurin:21-jdk-alpine
ARG JAR_FILE=target/order-0.0.6-SNAPSHOT.jar

WORKDIR /app
COPY ${JAR_FILE} /app/order.jar

# Default olarak uygulama jar dosyasını çalıştır
ENTRYPOINT ["java", "-jar", "/app/order.jar"]
