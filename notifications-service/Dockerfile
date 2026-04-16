FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -pl notifications-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/notifications-service/target/notifications-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
