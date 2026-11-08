# InformeClaro · microservicios
# Una sola imagen con todos los ejecutables:
#   - docker-compose arranca cada microservicio en su propio contenedor (SERVICIO=<nombre>);
#   - en Render (un único servicio web con poca memoria) se usa el lanzador-demo: los 5 módulos en
#     una JVM, cada uno con su contexto y su servidor gRPC, comunicándose por gRPC/HTTP/2 en 127.0.0.1.

FROM eclipse-temurin:21-jdk AS construccion
WORKDIR /fuente
COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
    && ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=construccion /fuente/servicio-usuarios/target/servicio-usuarios-exec.jar /app/servicio-usuarios.jar
COPY --from=construccion /fuente/servicio-documentos/target/servicio-documentos-exec.jar /app/servicio-documentos.jar
COPY --from=construccion /fuente/servicio-interpretacion/target/servicio-interpretacion-exec.jar /app/servicio-interpretacion.jar
COPY --from=construccion /fuente/servicio-informes/target/servicio-informes-exec.jar /app/servicio-informes.jar
COPY --from=construccion /fuente/infraestructura/target/infraestructura-exec.jar /app/infraestructura.jar
COPY --from=construccion /fuente/lanzador-demo/target/lanzador-demo.jar /app/lanzador-demo.jar
COPY docker/iniciar.sh /app/iniciar.sh
RUN sed -i 's/\r$//' /app/iniciar.sh && chmod +x /app/iniciar.sh

# Memoria ajustada para que todo quepa en una instancia de 512 MB.
ENV JAVA_OPTS="-XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss256k -XX:ReservedCodeCacheSize=40m -XX:MaxMetaspaceSize=170m -Xmx200m"
EXPOSE 8000
CMD ["/app/iniciar.sh"]
