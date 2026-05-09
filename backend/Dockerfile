FROM eclipse-temurin:17

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/protein-viz-backend-1.0.0.jar"]
