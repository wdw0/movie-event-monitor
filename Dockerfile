# Estágio de build: compila, roda os testes Java e reúne as dependências.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package \
 && mvn -q -B dependency:copy-dependencies -DoutputDirectory=target/lib -DincludeScope=runtime

# Estágio de execução: só JRE, classes compiladas e dependências.
FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/lib ./lib
USER app
EXPOSE 8080
# A classe principal é informada em `command:` (um container por componente).
ENTRYPOINT ["java", "-cp", "/app/classes:/app/lib/*"]
CMD ["br.ufes.moviemonitor.dashboard.DashboardServer"]
