# Сборка: JDK + Maven Wrapper. Зависимости качаются отдельным слоем и кешируются, пока не меняется pom.xml.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY --chmod=755 mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Запуск: только JRE и собранный jar, процесс не от root.
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/target/students.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
