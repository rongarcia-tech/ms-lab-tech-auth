# ============================
# Etapa 1: Build con Maven
# ============================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar pom y resolver dependencias (cacheable)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copiar código fuente y construir
COPY src ./src
RUN mvn -B -DskipTests clean package

# ============================
# Etapa 2: Runtime ligero
# ============================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el JAR generado
COPY --from=build /app/target/*.jar app.jar

# Puerto interno donde escucha ms-auth
EXPOSE 8080

# Permite tunear la JVM desde variables de entorno
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
