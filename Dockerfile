FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
COPY fitme-*.jar app.jar
RUN mkdir -p logs && chown -R appuser:appuser app.jar logs
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
