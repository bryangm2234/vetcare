FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN sh mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre
RUN useradd --system --no-create-home vetcare
WORKDIR /app
COPY --from=build /app/target/ms-facturacion.jar app.jar
USER vetcare
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
