FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY . .

RUN chmod +x mvnw && ./mvnw -Pprod -DskipTests -ntp --batch-mode verify

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    SPRING_PROFILES_ACTIVE=prod,api-docs \
    SPRING_DOCKER_COMPOSE_ENABLED=false \
    JAVA_OPTS="" \
    JHIPSTER_SLEEP=0

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "echo \"The application will start in ${JHIPSTER_SLEEP}s...\" && sleep ${JHIPSTER_SLEEP} && exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
