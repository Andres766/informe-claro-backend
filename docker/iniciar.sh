#!/bin/sh
# SERVICIO=<nombre>  -> arranca solo ese microservicio (modo docker-compose, un contenedor por servicio).
# sin SERVICIO       -> lanzador-demo: los 4 microservicios y el orquestador en una JVM (modo Render).
#                       Se siguen comunicando por gRPC sobre HTTP/2 en 127.0.0.1; solo el orquestador
#                       expone $PORT con la API REST.
set -e

if [ -n "$SERVICIO" ]; then
  exec java $JAVA_OPTS -jar "/app/$SERVICIO.jar"
fi

exec java $JAVA_OPTS -jar /app/lanzador-demo.jar
