"""API Backend (FastAPI) — orquestador central de InformeClaro.

Flujo de POST /api/informes:
  1. recibe el PDF  2. extrae texto  3-5. prompt con guardrails + LLM
  6. persiste valores y preguntas (sin el PDF) y responde al frontend.
"""
import logging
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from . import models
from .auth import router as auth_router
from .auth import usuario_actual
from .config import settings
from .database import Base, engine, get_db, migrar_columnas_nuevas
from .i18n import normalizar_idioma, t
from .schemas import InformeDetalleOut, InformeResumenOut
from .services.pdf_parser import PDFInvalidoError, anonimizar, extraer_texto
from .services.prompt_orchestrator import AVISO_MEDICO, interpretar

logging.basicConfig(level=logging.INFO)

@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(engine)
    migrar_columnas_nuevas()
    yield


app = FastAPI(title="InformeClaro API", version="0.2.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(auth_router)


def _detalle(informe: models.InformeCargado) -> InformeDetalleOut:
    return InformeDetalleOut(
        id=informe.id,
        nombre_archivo=informe.nombre_archivo,
        fecha_carga=informe.fecha_carga,
        motor=informe.motor,
        idioma=informe.idioma or "es",
        resumen=informe.resumen,
        valores=informe.valores,
        preguntas=informe.preguntas,
        aviso=AVISO_MEDICO,
    )


def idioma_peticion(accept_language: str | None = Header(None)) -> str:
    """Idioma de los mensajes de la API según el encabezado Accept-Language."""
    return normalizar_idioma(accept_language)


def _informe_del_usuario(informe_id: int, usuario: models.Usuario, db: Session, idioma: str) -> models.InformeCargado:
    informe = db.scalar(
        select(models.InformeCargado)
        .where(models.InformeCargado.id == informe_id, models.InformeCargado.usuario_id == usuario.id)
        .options(selectinload(models.InformeCargado.valores), selectinload(models.InformeCargado.preguntas))
    )
    if not informe:
        raise HTTPException(404, t("informe_no_encontrado", idioma))
    return informe


@app.get("/api/salud")
def salud():
    return {"estado": "ok", "motor": settings.llm_model if settings.anthropic_api_key else "demo (reglas)"}


@app.post("/api/informes", response_model=InformeDetalleOut, status_code=201)
async def subir_informe(
    archivo: UploadFile = File(...),
    idioma_informe: str | None = Form(None, alias="idioma"),
    db: Session = Depends(get_db),
    usuario: models.Usuario = Depends(usuario_actual),
    idioma: str = Depends(idioma_peticion),
):
    if archivo.content_type != "application/pdf" and not (archivo.filename or "").lower().endswith(".pdf"):
        raise HTTPException(415, t("solo_pdf", idioma))

    contenido = await archivo.read()
    if len(contenido) > settings.max_pdf_mb * 1024 * 1024:
        raise HTTPException(413, t("pdf_grande", idioma, mb=settings.max_pdf_mb))

    try:
        texto = extraer_texto(contenido)
    except PDFInvalidoError as exc:
        raise HTTPException(422, t(exc.clave, idioma))
    del contenido  # el PDF original no se conserva

    # La explicación se genera en el idioma pedido en el formulario; si no viene, el del navegador.
    idioma_explicacion = normalizar_idioma(idioma_informe) if idioma_informe else idioma
    resultado = interpretar(anonimizar(texto), idioma_explicacion)
    if not resultado.valores:
        raise HTTPException(422, t("sin_valores", idioma))

    informe = models.InformeCargado(
        usuario_id=usuario.id,
        nombre_archivo=archivo.filename or "informe.pdf",
        resumen=resultado.resumen,
        motor=resultado.motor,
        idioma=idioma_explicacion,
        valores=[
            models.ValorExamen(
                nombre_valor=v.nombre_valor, unidad=v.unidad, valor=v.valor,
                rango_referencia=v.rango_referencia, estado=v.estado, explicacion_ia=v.explicacion,
            )
            for v in resultado.valores
        ],
        preguntas=[models.PreguntaSugerida(texto_pregunta=p) for p in resultado.preguntas],
    )
    db.add(informe)
    db.commit()
    return _detalle(informe)


@app.get("/api/informes", response_model=list[InformeResumenOut])
def listar_informes(db: Session = Depends(get_db), usuario: models.Usuario = Depends(usuario_actual)):
    informes = db.scalars(
        select(models.InformeCargado)
        .where(models.InformeCargado.usuario_id == usuario.id)
        .options(selectinload(models.InformeCargado.valores))
        .order_by(models.InformeCargado.fecha_carga.desc())
    ).all()
    return [
        InformeResumenOut(
            id=i.id, nombre_archivo=i.nombre_archivo, fecha_carga=i.fecha_carga, motor=i.motor,
            idioma=i.idioma or "es",
            total_valores=len(i.valores),
            fuera_de_rango=sum(v.estado in ("alto", "bajo") for v in i.valores),
        )
        for i in informes
    ]


@app.get("/api/informes/{informe_id}", response_model=InformeDetalleOut)
def ver_informe(
    informe_id: int,
    db: Session = Depends(get_db),
    usuario: models.Usuario = Depends(usuario_actual),
    idioma: str = Depends(idioma_peticion),
):
    return _detalle(_informe_del_usuario(informe_id, usuario, db, idioma))


@app.delete("/api/informes/{informe_id}", status_code=204)
def eliminar_informe(
    informe_id: int,
    db: Session = Depends(get_db),
    usuario: models.Usuario = Depends(usuario_actual),
    idioma: str = Depends(idioma_peticion),
):
    db.delete(_informe_del_usuario(informe_id, usuario, db, idioma))
    db.commit()
