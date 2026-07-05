FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline --no-transfer-progress
COPY src ./src

FROM maven:3.9.6-eclipse-temurin-21-alpine
WORKDIR /app
COPY --from=builder /root/.m2 /root/.m2
COPY --from=builder /app /app

RUN mkdir -p reports logs

CMD ["mvn", "test", "-Denv=docker", "-DsuiteFile=testng.xml", "--no-transfer-progress"]
