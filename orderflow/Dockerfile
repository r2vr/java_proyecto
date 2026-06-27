# ---- build stage: compile and package the executable JAR ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy only the POMs first so dependency resolution is cached as its own layer
# and isn't invalidated every time source code changes.
COPY pom.xml .
COPY orderflow-domain/pom.xml orderflow-domain/
COPY orderflow-application/pom.xml orderflow-application/
COPY orderflow-infrastructure/pom.xml orderflow-infrastructure/
COPY orderflow-bootstrap/pom.xml orderflow-bootstrap/
RUN mvn -q -B -ntp dependency:go-offline
COPY . .
RUN mvn -q -B -ntp -DskipTests package

# ---- run stage: small image with only the JRE and the app ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/orderflow-bootstrap/target/orderflow.jar app.jar
# The free tier gives ~512 MB; cap the heap so the JVM fits.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
