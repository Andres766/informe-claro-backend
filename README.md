# InformeClaro — Backend en microservicios (monorepositorio)

API de **InformeClaro**, el traductor de resultados de laboratorio a lenguaje sencillo. El paciente
sube el PDF de su examen; el sistema extrae cada valor, lo explica comparándolo con el rango de
referencia y genera preguntas para la próxima consulta. **Nunca diagnostica ni recomienda
tratamiento**: la decisión clínica es 100 % del médico.

Proyecto final individual — Programación Orientada a la Web, Ingeniería de Software, UCC Pasto.

- Frontend (React): https://github.com/Andres766/informe-claro-frontend
- Stack: Java 21 · Spring Boot 4 · gRPC 1.85 (HTTP/2, Netty) · Protocol Buffers · JPA/Hibernate ·
  PostgreSQL · PDFBox · API de Gemini o Claude (REST) · JWT + bcrypt · Docker.

---

## Arquitectura

```
                         REST (JSON)                           REST (HTTPS)
 Navegador (React) ───────────────────▶ ┌─────────────────┐            ┌──────────────────┐
                                        │ infraestructura │            │   API de IA · 3.º │
                                        │  (orquestador)  │            │ Gemini o Claude  │
                                        └───┬────┬────┬───┘            └────────▲─────────┘
                       gRPC / HTTP/2        │    │    │                          │ REST
             ┌──────────────────────────────┘    │    └───────────────┐          │
             ▼                         ▼         ▼                    ▼          │
   ┌──────────────────┐  ┌───────────────────┐  ┌──────────────────────┐  ┌──────────────────┐
   │ servicio-usuarios│  │servicio-documentos│◀─┤servicio-interpretacion├──┘  │ servicio-informes│
   │  cuentas · JWT   │  │ PDF · anonimizar  │  │ IA · reglas ·         │     │    historial     │
   │     :9091        │  │ extraer · :9092   │  │ guardrails · :9093    │     │      :9094       │
   └────────┬─────────┘  └─────────┬─────────┘  └──────────┬───────────┘     └────────┬─────────┘
            │      gRPC / HTTP/2 ──┘ (interpretación pide valores y clasificaciones)      │
            └───────────────────────────────┬───────────────┴────────────────────────────┘
                                            ▼
                                ┌───────────────────────┐
                                │ PostgreSQL (una sola) │
                                └───────────────────────┘
```

### Reglas de comunicación

| Entre | Protocolo | Dónde está |
|---|---|---|
| Microservicio ⇄ microservicio, orquestador ⇄ microservicio | **gRPC sobre HTTP/2** (Netty, h2c en la red interna) | Contratos en `contratos-grpc/src/main/proto/*.proto`; clientes en `infraestructura/.../cliente/` y `servicio-interpretacion/.../cliente/` |
| Sistema ⇄ API de terceros (Google Gemini o Anthropic Claude) | **REST** (HTTPS + JSON) | `servicio-interpretacion/.../externo/GeminiRestCliente.java` y `AnthropicRestCliente.java` (interfaz común `ClienteLlm`) |
| Navegador ⇄ sistema | REST (JSON) | Solo `infraestructura` expone HTTP; los microservicios **no** tienen servidor web |

### Módulos

| Módulo | Responsabilidad | Tablas (base única) |
|---|---|---|
| `contratos-grpc` | Contratos `.proto` compartidos y código gRPC generado | — |
| `comun` | Servidor gRPC reutilizable, canales HTTP/2, errores entre servicios, conexión a la base | — |
| `servicio-usuarios` | Registro, inicio de sesión, contraseñas bcrypt, tokens JWT | `usuario` |
| `servicio-documentos` | Lee el PDF **en memoria** (nunca lo guarda), oculta datos personales, extrae y clasifica valores | `registro_procesamiento` (bitácora sin contenido) |
| `servicio-interpretacion` | Explica cada valor (Gemini/Claude por REST o motor por reglas), guardrails clínicos, preguntas, aviso médico | `registro_interpretacion` (bitácora) |
| `servicio-informes` | Historial de cada usuario (valores, explicación y preguntas) | `informe_cargado`, `valor_examen`, `pregunta_sugerida` |
| `infraestructura` | **Orquestador**: API REST del navegador, autenticación, coordinación del flujo, traducción de errores, CORS | — |
| `lanzador-demo` | Arranca todos los módulos en **una sola JVM** para servidores con poca memoria (Render gratis, 512 MB). Cada módulo conserva su contexto, su configuración y su servidor gRPC; se siguen hablando por gRPC/HTTP/2 en `127.0.0.1` | — |

### Capas de cada microservicio

```
servicio-x/src/main/java/co/edu/ucc/informeclaro/x/
├── model/        entidades JPA
├── repository/   acceso a datos (Spring Data)
├── dto/          objetos de transferencia (records)
├── service/      reglas de negocio
├── controller/   controlador interno: recibe DTOs y coordina los servicios
└── api/          XxxGrpcApi.java ← la API gRPC: única puerta de entrada desde los demás módulos;
                  traduce protobuf ⇄ DTO y delega en el controlador
```

Los errores de negocio viajan como `Status` de gRPC (por ejemplo `NOT_FOUND` con la clave
`informe_no_encontrado`); el orquestador los convierte en el HTTP y el mensaje traducido de siempre.

### Flujo de `POST /api/informes`

1. **infraestructura** valida el token (gRPC → `servicio-usuarios`), el tipo y el tamaño del archivo.
2. gRPC → `servicio-documentos`: extrae el texto del PDF en memoria y oculta los datos personales.
3. gRPC → `servicio-interpretacion`, que:
   - con API key, llama a la IA por **REST** con salida estructurada (JSON Schema): Gemini si hay `GEMINI_API_KEY`, si no Claude con `ANTHROPIC_API_KEY`; si Gemini está saturado, reintenta y prueba un modelo de respaldo; sin key o si todo falla, usa el motor por reglas;
   - pide por gRPC a `servicio-documentos` el estado de cada valor (lo decide el código, no el LLM);
   - aplica los guardrails y devuelve explicación, resumen y preguntas.
4. gRPC → `servicio-informes`: guarda el resultado (sin el PDF).
5. gRPC → `servicio-interpretacion`: aviso médico en el idioma del informe; respuesta REST al navegador.

### Guardrails clínicos (tres capas)

1. **Prompt de sistema** con la línea clínica (explicar sí; diagnosticar, interpretar causas o recomendar tratamiento, no).
2. **Decisión determinista**: el estado normal/alto/bajo lo calcula `servicio-documentos` comparando números.
3. **Filtro de salida** en español e inglés: frases de diagnóstico, tratamiento o alarma se reemplazan por un texto seguro.

---

## Cómo ejecutarlo

### Con Docker (recomendado)

```bash
docker compose up --build         # PostgreSQL + 4 microservicios + orquestador
# API REST en http://localhost:8000  (el frontend en http://localhost:5173 ya apunta ahí)
```

Variables opcionales: `GEMINI_API_KEY` o `ANTHROPIC_API_KEY` (sin ninguna, modo demo por reglas), `JWT_SECRET` (32 caracteres o más),
`CORS_ORIGINS`.

### Sin Docker

Requisitos: Java 21+ y un PostgreSQL en `127.0.0.1:5435` (base, usuario y clave `informeclaro`) o
`DATABASE_URL` apuntando a uno.

```bash
./mvnw package                                     # compila y prueba todos los módulos
# Opción A: un proceso por microservicio
java -jar servicio-usuarios/target/servicio-usuarios-exec.jar
java -jar servicio-documentos/target/servicio-documentos-exec.jar
java -jar servicio-interpretacion/target/servicio-interpretacion-exec.jar
java -jar servicio-informes/target/servicio-informes-exec.jar
java -jar infraestructura/target/infraestructura-exec.jar
# Opción B: todos en una JVM (siguen comunicándose por gRPC)
java -jar lanzador-demo/target/lanzador-demo.jar
```

Cada módulo lee su propia configuración (`servicio-usuarios.yml`, `infraestructura.yml`, …).

### Pruebas

- `./mvnw test` — 79 pruebas: cada microservicio se prueba a través de su **API gRPC real** (HTTP/2) con
  una base H2; los clientes REST de Gemini y Anthropic se prueban sin salir a internet.
- `pytest pruebas-contrato` — pruebas de **contrato** contra el sistema en marcha: verifican que la API REST
  que usa el frontend responde igual que la versión monolítica (rutas, códigos, JSON y mensajes).
- Compatibilidad: las contraseñas (bcrypt `$2b$`) y los tokens JWT emitidos por la versión anterior siguen siendo válidos.

---

## Endpoints REST (orquestador)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/salud` | Estado, motor de interpretación e idiomas |
| `POST` | `/api/auth/registro` | Crea una cuenta y devuelve el token |
| `POST` | `/api/auth/login` | Inicia sesión y devuelve el token |
| `GET` | `/api/auth/yo` | Usuario de la sesión |
| `POST` | `/api/informes` | Sube un PDF (`archivo`, opcional `idioma`: `es`/`en`) y devuelve la interpretación |
| `GET` | `/api/informes` | Historial del usuario |
| `GET` | `/api/informes/{id}` | Detalle de un informe |
| `DELETE` | `/api/informes/{id}` | Elimina un informe |

Los mensajes de error salen en el idioma del encabezado `Accept-Language` (español por defecto).

## Despliegue (Render)

Servicio web con **runtime Docker** sobre este repositorio. Como el plan gratuito tiene 512 MB, el
contenedor usa el `lanzador-demo` (los 5 módulos en una JVM, ~200 MB): los microservicios se siguen
comunicando por gRPC/HTTP/2 en `127.0.0.1` y solo el orquestador expone `$PORT`. Con más memoria se
puede desplegar cada servicio por separado con la misma imagen y la variable `SERVICIO=<nombre>`.

Variables: `DATABASE_URL` (la URL de Postgres de Render, Neon o Supabase), `JWT_SECRET`, `CORS_ORIGINS`
(URL del frontend), `GEMINI_API_KEY` (IA real; opcional, sin ella modo demo), `GEMINI_MODEL` y `GEMINI_MODEL_RESPALDO` (opcionales),
`ANTHROPIC_API_KEY` (alternativa a Gemini), `IDIOMA_POR_DEFECTO` (opcional).
Health check: `/api/salud`.
