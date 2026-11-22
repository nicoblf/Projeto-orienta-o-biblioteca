# Contêiner do BACKEND (API em Java). Compila com o JDK e executa só com o JRE.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /backend
COPY src ./src
RUN mkdir out && javac -encoding UTF-8 -d out $(find src -name "*.java")

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /backend/out ./out
# Driver JDBC do PostgreSQL (só é carregado em tempo de execução)
RUN mkdir -p /app/lib
ADD --chmod=644 https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar /app/lib/postgresql.jar
ENV PORT=8080
EXPOSE 8080
USER nobody
CMD ["java", "-cp", "out:lib/postgresql.jar", "biblioteca.Main"]
