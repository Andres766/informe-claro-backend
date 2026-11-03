# InformeClaro — Backend (API)

API de **InformeClaro**, el traductor de resultados de laboratorio a lenguaje sencillo.
El paciente sube el PDF de su examen; la API extrae cada valor, lo explica comparándolo con el
rango de referencia y genera preguntas para la próxima consulta. **Nunca diagnostica ni
recomienda tratamiento**: la decisión clínica es 100 % del médico.

Proyecto final individual — Programación Orientada a la Web, Ingeniería de Software, UCC Pasto.

- Frontend (React): https://github.com/Andres766/informe-claro-frontend
- API desplegada: https://informe-claro.onrender.com (documentación interactiva en `/docs`)

Stack: Python · FastAPI · SQLAlchemy · PostgreSQL · pdfplumber · API de Claude · JWT + bcrypt.

---

## Cómo ejecutarlo

Requisitos: Python 3.11+.

```bash
python -m venv .venv
.venv\Scripts\activate          # Linux/Mac: source .venv/bin/activate
pip install -r requirements.txt
copy .env.example .env          # opcional: poner ANTHROPIC_API_KEY y JWT_SECRET
uvicorn app.main:app --reload   # http://localhost:8000  (documentación: /docs)
```

- Sin `ANTHROPIC_API_KEY` la API funciona en **modo demo**: extrae los valores con reglas
  (regex) y genera explicaciones con plantillas. Con la key usa Claude.
- PDF de prueba con datos ficticios: `samples/informe_ejemplo.pdf` y `samples/informe_ejemplo_2.pdf`
  (se regeneran con `python scripts/generar_pdf_ejemplo.py`).
- Pruebas: `pytest` (20 pruebas).

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/salud` | Estado del servicio y motor de interpretación |
| `POST` | `/api/auth/registro` | Crea una cuenta y devuelve el token |
| `POST` | `/api/auth/login` | Inicia sesión y devuelve el token |
| `GET` | `/api/auth/yo` | Datos del usuario de la sesión |
| `POST` | `/api/informes` | Sube un PDF y devuelve la interpretación |
| `GET` | `/api/informes` | Historial del usuario |
| `GET` | `/api/informes/{id}` | Detalle de un informe |
| `DELETE` | `/api/informes/{id}` | Elimina un informe |

Todas las rutas de `/api/informes` exigen la cabecera `Authorization: Bearer <token>`.

---

## Arquitectura

```
Frontend React ──► FastAPI (orquestador) ──► Servicio de parsing PDF (pdfplumber)
                          │                ──► Orquestador de prompt ──► API LLM (Claude)
                          └──► Base de datos (SQLite en desarrollo / PostgreSQL en producción)
```

| Capa | Archivo(s) |
|---|---|
| Aplicación / API | `app/main.py` — informes y salud |
| Autenticación | `app/auth.py`, `app/security.py` — registro, login, JWT, bcrypt |
| Servicio de parsing | `app/services/pdf_parser.py` — extracción, anonimización, clasificación por rango |
| Orquestador de prompt | `app/services/prompt_orchestrator.py` — prompt con guardrails, llamada al LLM, filtro de salida |
| Datos | `app/models.py` — `Usuario`, `InformeCargado`, `ValorExamen`, `PreguntaSugerida` |

### Flujo de `POST /api/informes`

1. El navegador envía el PDF (multipart).
2. `pdfplumber` extrae el texto **en memoria**; el PDF nunca se escribe a disco ni se guarda.
3. Se ocultan los datos personales (nombre, documento, correo, números largos) y se arma el prompt.
4. Se llama a Claude con **salida estructurada** (esquema Pydantic): valores, explicaciones y preguntas.
5. El backend calcula el estado de cada valor (normal / alto / bajo) y aplica el filtro de guardrails.
6. Se guarda en la base de datos y se devuelve al frontend con el aviso médico.

### Guardrails clínicos (en tres capas)

1. **Prompt de sistema**: define qué SÍ hace (explicar, comparar con el rango, sugerir preguntas)
   y qué NO hace (diagnosticar, interpretar causas, recomendar tratamientos, generar alarma).
2. **Decisión determinista**: si un valor está fuera de rango lo decide el código comparando
   números, no el LLM, para que sea siempre verificable.
3. **Filtro de salida**: si la explicación contiene frases de diagnóstico ("usted tiene…"),
   de tratamiento ("tome el medicamento…") o alarmistas ("grave", "urgente"), se reemplaza por
   un texto seguro que remite al médico.

---

## Despliegue (Render)

| Campo | Valor |
|---|---|
| Build Command | `pip install -r requirements.txt` |
| Start Command | `uvicorn app.main:app --host 0.0.0.0 --port $PORT` |
| Health Check Path | `/api/salud` |

Variables de entorno: `DATABASE_URL`, `CORS_ORIGINS` (URL del frontend), `JWT_SECRET`,
`PYTHON_VERSION`, `ANTHROPIC_API_KEY` (opcional).

## Pendiente

- [ ] Probar el prompt con informes reales de distintos laboratorios y ajustar los guardrails.
- [ ] Soporte a PDF escaneados (OCR).
- [ ] Cifrado en reposo de las explicaciones en la base de datos.
- [ ] Recuperación de contraseña por correo.
- [ ] Migrar la base de datos gratuita de Render (caduca a los 30 días) a Neon/Supabase.
