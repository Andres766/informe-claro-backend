"""Pruebas de contrato de la API REST pública (la que usa el frontend).

Se ejecutan contra el sistema completo en marcha (orquestador + microservicios + base de datos)
y verifican que las rutas, los códigos HTTP, el JSON y los mensajes sean los mismos que tenía
la versión monolítica. Uso:

    docker compose up -d --build
    pip install pytest httpx
    pytest pruebas-contrato            # BASE_URL=http://localhost:8000 por defecto
"""
import os
import time
import uuid
from pathlib import Path

import httpx
import pytest

BASE = os.environ.get("BASE_URL", "http://localhost:8000")
MUESTRAS = Path(__file__).resolve().parent.parent / "samples"
EN = {"Accept-Language": "en-US,en;q=0.9"}


def correo(prefijo="prueba"):
    return f"{prefijo}-{uuid.uuid4().hex[:10]}@prueba.co"


@pytest.fixture(scope="session")
def api():
    with httpx.Client(base_url=BASE, timeout=120) as c:
        # Los microservicios tardan en arrancar: se espera a que interpretación (gRPC) responda en
        # /api/salud y a que usuarios (gRPC) conteste un login con 401 en vez de 503.
        for _ in range(90):
            try:
                listo = c.get("/api/salud").json().get("motor") != "no disponible" and c.post(
                    "/api/auth/login", json={"email": "sonda@prueba.co", "password": "x"}).status_code == 401
                if listo:
                    break
            except (httpx.HTTPError, ValueError):
                pass
            time.sleep(2)
        yield c


@pytest.fixture()
def sesion(api):
    r = api.post("/api/auth/registro", json={"nombre": "Paciente Prueba", "email": correo(), "password": "clave-segura-1"})
    assert r.status_code == 201, r.text
    return {"Authorization": f"Bearer {r.json()['access_token']}"}


def subir(api, cabeceras, archivo="informe_ejemplo.pdf", **datos):
    with (MUESTRAS / archivo).open("rb") as f:
        return api.post("/api/informes", headers=cabeceras, data=datos or None,
                        files={"archivo": (archivo, f, "application/pdf")})


# --- Salud --------------------------------------------------------------------

def test_salud(api):
    salud = api.get("/api/salud").json()
    assert salud["estado"] == "ok"
    assert salud["idiomas"] == ["es", "en"] and salud["idioma_por_defecto"] == "es"
    assert salud["motor"]


# --- Autenticación -------------------------------------------------------------

def test_registro_login_y_yo(api):
    email = correo()
    r = api.post("/api/auth/registro", json={"nombre": "  Ana María ", "email": email.upper(), "password": "clave-segura-1"})
    assert r.status_code == 201
    cuerpo = r.json()
    assert set(cuerpo) == {"access_token", "token_type", "usuario"}
    assert cuerpo["token_type"] == "bearer"
    assert cuerpo["usuario"] == {"id": cuerpo["usuario"]["id"], "nombre": "Ana María", "email": email}

    r = api.post("/api/auth/login", json={"email": email, "password": "clave-segura-1"})
    assert r.status_code == 200
    yo = api.get("/api/auth/yo", headers={"Authorization": f"Bearer {r.json()['access_token']}"})
    assert yo.status_code == 200 and yo.json()["email"] == email


def test_sin_sesion_no_hay_acceso(api):
    r = api.get("/api/informes")
    assert r.status_code == 401 and r.json() == {"detail": "Inicia sesión para continuar"}
    assert r.headers.get("www-authenticate") == "Bearer"
    r = api.get("/api/informes", headers={"Authorization": "Bearer token-falso", **EN})
    assert r.status_code == 401 and r.json() == {"detail": "Sign in to continue"}


def test_errores_de_registro_y_login(api):
    email = correo()
    datos = {"nombre": "Paciente", "email": email, "password": "clave-segura-1"}
    assert api.post("/api/auth/registro", json=datos).status_code == 201
    r = api.post("/api/auth/registro", json={**datos, "email": email.upper()}, headers=EN)
    assert r.status_code == 409 and r.json()["detail"] == "An account with that email already exists"

    mal = api.post("/api/auth/login", json={"email": email, "password": "equivocada"})
    no_existe = api.post("/api/auth/login", json={"email": correo("nadie"), "password": "equivocada"})
    assert mal.status_code == no_existe.status_code == 401
    assert mal.json() == no_existe.json() == {"detail": "Correo o contraseña incorrectos"}


@pytest.mark.parametrize("cambio,campo", [
    ({"email": "no-es-correo"}, "email"),
    ({"password": "corta"}, "password"),
    ({"nombre": "A"}, "nombre"),
])
def test_validacion_del_registro_con_el_formato_que_lee_el_frontend(api, cambio, campo):
    datos = {"nombre": "Paciente", "email": correo(), "password": "clave-segura-1", **cambio}
    r = api.post("/api/auth/registro", json=datos)
    assert r.status_code == 422
    assert r.json()["detail"][0]["loc"][-1] == campo


# --- Informes -------------------------------------------------------------------

def test_flujo_completo_en_espanol(api, sesion):
    r = subir(api, sesion)
    assert r.status_code == 201, r.text
    informe = r.json()
    assert set(informe) == {"id", "nombre_archivo", "fecha_carga", "motor", "idioma", "resumen", "valores", "preguntas", "aviso"}
    assert informe["idioma"] == "es" and len(informe["valores"]) == 12
    assert set(informe["valores"][0]) == {"id", "nombre_valor", "unidad", "valor", "rango_referencia", "estado", "explicacion_ia"}
    estados = {v["nombre_valor"]: v["estado"] for v in informe["valores"]}
    assert estados["Glucosa en ayunas"] == "alto" and estados["Hemoglobina"] == "bajo"
    assert "médico" in informe["aviso"]
    assert all(set(p) == {"id", "texto_pregunta"} for p in informe["preguntas"])

    historial = api.get("/api/informes", headers=sesion).json()
    assert set(historial[0]) == {"id", "nombre_archivo", "fecha_carga", "motor", "idioma", "total_valores", "fuera_de_rango"}
    assert historial[0]["id"] == informe["id"] and historial[0]["fuera_de_rango"] == 4

    assert api.get(f"/api/informes/{informe['id']}", headers=sesion).status_code == 200
    borrado = api.delete(f"/api/informes/{informe['id']}", headers=sesion)
    assert borrado.status_code == 204 and borrado.content == b""
    r = api.get(f"/api/informes/{informe['id']}", headers=sesion)
    assert r.status_code == 404 and r.json() == {"detail": "Informe no encontrado"}


def test_explicacion_en_ingles_y_preguntas_con_concordancia(api, sesion):
    r = subir(api, {**sesion, **EN}, "informe_ejemplo_2.pdf", idioma="en")
    informe = r.json()
    assert r.status_code == 201 and informe["idioma"] == "en" and len(informe["valores"]) == 12
    assert informe["aviso"].startswith("This explanation is for information only")
    assert informe["resumen"].startswith("Your report has 12 values")

    es = subir(api, sesion, "informe_ejemplo_2.pdf").json()
    preguntas = [p["texto_pregunta"] for p in es["preguntas"]]
    assert "¿Qué significa para mí que mis triglicéridos estén por encima del rango de referencia?" in preguntas
    assert "¿Qué significa para mí que mi vitamina D esté por debajo del rango de referencia?" in preguntas


def test_informe_en_ingles(api, sesion):
    r = subir(api, sesion, "lab_report_example.pdf", idioma="en")
    informe = r.json()
    assert r.status_code == 201 and len(informe["valores"]) == 10
    colesterol = next(v for v in informe["valores"] if v["nombre_valor"] == "Total cholesterol")
    assert colesterol["estado"] == "alto"
    assert colesterol["explicacion_ia"].startswith("This test measures your total cholesterol")


def test_un_usuario_no_ve_ni_borra_informes_de_otro(api, sesion):
    propio = subir(api, sesion).json()
    r = api.post("/api/auth/registro", json={"nombre": "Intruso", "email": correo("intruso"), "password": "clave-segura-1"})
    intruso = {"Authorization": f"Bearer {r.json()['access_token']}"}
    assert api.get("/api/informes", headers=intruso).json() == []
    assert api.get(f"/api/informes/{propio['id']}", headers=intruso).status_code == 404
    assert api.delete(f"/api/informes/{propio['id']}", headers=intruso).status_code == 404
    assert api.get(f"/api/informes/{propio['id']}", headers=sesion).status_code == 200


def test_errores_de_carga(api, sesion):
    r = api.post("/api/informes", headers={**sesion, **EN}, files={"archivo": ("notes.txt", b"hi", "text/plain")})
    assert r.status_code == 415 and r.json()["detail"] == "Only PDF files are accepted"
    r = api.post("/api/informes", headers=sesion, files={"archivo": ("roto.pdf", b"no es pdf", "application/pdf")})
    assert r.status_code == 422 and r.json()["detail"] == "No se pudo leer el PDF. ¿Es un archivo válido?"
    r = subir(api, {**sesion, **EN}, idioma="fr")
    assert r.status_code == 422 and r.json()["detail"] == "Unsupported language. Use “es” or “en”."


def test_cors_para_el_frontend(api):
    r = api.options("/api/auth/login", headers={
        "Origin": "http://localhost:5173", "Access-Control-Request-Method": "POST",
        "Access-Control-Request-Headers": "content-type,authorization"})
    assert r.status_code == 200
    assert r.headers.get("access-control-allow-origin") == "http://localhost:5173"
