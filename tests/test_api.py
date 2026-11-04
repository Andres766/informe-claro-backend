import os
from pathlib import Path

# Base de datos temporal y modo demo (sin API key) para las pruebas.
os.environ["DATABASE_URL"] = "sqlite:///./test_informeclaro.db"
os.environ["ANTHROPIC_API_KEY"] = ""

import pytest
from fastapi.testclient import TestClient

from app.database import engine
from app.main import app
from app.services.pdf_parser import anonimizar, clasificar, extraer_valores_por_reglas
from app.services.prompt_orchestrator import aplicar_guardrails

PDF_EJEMPLO = Path(__file__).resolve().parent.parent / "samples" / "informe_ejemplo.pdf"


@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        r = c.post("/api/auth/registro", json={
            "nombre": "Paciente Prueba", "email": "prueba@informeclaro.co", "password": "clave-segura-1",
        })
        c.headers["Authorization"] = f"Bearer {r.json()['access_token']}"
        yield c
    engine.dispose()
    Path("test_informeclaro.db").unlink(missing_ok=True)


@pytest.mark.parametrize("valor,rango,esperado", [
    ("112", "70 - 100", "alto"),
    ("65", "70 - 100", "bajo"),
    ("85", "70 - 100", "normal"),
    ("215", "< 200", "alto"),
    ("48", "> 40", "normal"),
    ("0,9", "0,6 - 1,2", "normal"),
    ("positivo", "negativo", "sin_rango"),
])
def test_clasificar(valor, rango, esperado):
    assert clasificar(valor, rango) == esperado


def test_anonimizar_oculta_datos_personales():
    texto = anonimizar("Paciente: Juan Pérez\nCorreo juan@mail.com\nGlucosa 90 mg/dL 70 - 100\nID 1085123456")
    assert "Juan" not in texto and "juan@mail.com" not in texto and "1085123456" not in texto
    assert "Glucosa 90 mg/dL 70 - 100" in texto


def test_extrae_nombres_con_numeros_y_coma_decimal():
    valores = extraer_valores_por_reglas("Vitamina B12 420 pg/mL 200 - 900\nCreatinina 1,3 mg/dL 0,7 - 1,2")
    assert [(v.nombre_valor, v.valor) for v in valores] == [("Vitamina B12", "420"), ("Creatinina", "1,3")]


@pytest.mark.parametrize("texto", [
    "Usted tiene diabetes.",
    "Te recomiendo tomar metformina.",
    "Tome el medicamento cada 8 horas.",
    "Este valor es peligroso.",
])
def test_guardrails_bloquean_diagnosticos_y_tratamientos(texto):
    assert aplicar_guardrails(texto) != texto


def test_guardrails_dejan_pasar_explicaciones_neutrales():
    texto = "Este examen mide el azúcar en la sangre. Tu resultado está por encima del rango de referencia."
    assert aplicar_guardrails(texto) == texto


def test_flujo_completo(client):
    with PDF_EJEMPLO.open("rb") as f:
        r = client.post("/api/informes", files={"archivo": ("informe_ejemplo.pdf", f, "application/pdf")})
    assert r.status_code == 201, r.text
    informe = r.json()
    assert len(informe["valores"]) == 12
    estados = {v["nombre_valor"]: v["estado"] for v in informe["valores"]}
    assert estados["Glucosa en ayunas"] == "alto"
    assert estados["Hemoglobina"] == "bajo"
    assert informe["preguntas"] and "médico" in informe["aviso"]
    assert informe["idioma"] == "es"

    historial = client.get("/api/informes").json()
    assert historial[0]["id"] == informe["id"] and historial[0]["fuera_de_rango"] == 4
    assert historial[0]["idioma"] == "es"

    assert client.get(f"/api/informes/{informe['id']}").status_code == 200
    assert client.delete(f"/api/informes/{informe['id']}").status_code == 204
    assert client.get(f"/api/informes/{informe['id']}").status_code == 404


def test_rechaza_archivos_que_no_son_pdf(client):
    r = client.post("/api/informes", files={"archivo": ("notas.txt", b"hola", "text/plain")})
    assert r.status_code == 415


# --- Autenticación ---------------------------------------------------------

def _registrar(client, email, password="clave-segura-1"):
    r = client.post("/api/auth/registro", json={"nombre": "Otra Persona", "email": email, "password": password},
                    headers={"Authorization": ""})
    assert r.status_code == 201, r.text
    return {"Authorization": f"Bearer {r.json()['access_token']}"}


def test_sin_sesion_no_hay_acceso(client):
    sin_token = {"Authorization": ""}
    assert client.get("/api/informes", headers=sin_token).status_code == 401
    assert client.get("/api/informes", headers={"Authorization": "Bearer token-falso"}).status_code == 401


def test_registro_rechaza_correo_repetido_y_datos_invalidos(client):
    datos = {"nombre": "Paciente Prueba", "email": "PRUEBA@informeclaro.co", "password": "clave-segura-1"}
    assert client.post("/api/auth/registro", json=datos).status_code == 409
    assert client.post("/api/auth/registro", json={**datos, "email": "no-es-correo"}).status_code == 422
    assert client.post("/api/auth/registro", json={**datos, "email": "x@y.co", "password": "corta"}).status_code == 422


def test_login(client):
    ok = client.post("/api/auth/login", json={"email": "prueba@informeclaro.co", "password": "clave-segura-1"})
    assert ok.status_code == 200 and ok.json()["usuario"]["nombre"] == "Paciente Prueba"
    mal = client.post("/api/auth/login", json={"email": "prueba@informeclaro.co", "password": "equivocada"})
    no_existe = client.post("/api/auth/login", json={"email": "nadie@informeclaro.co", "password": "equivocada"})
    assert mal.status_code == no_existe.status_code == 401
    assert mal.json()["detail"] == no_existe.json()["detail"]


def test_un_usuario_no_ve_ni_borra_informes_de_otro(client):
    with PDF_EJEMPLO.open("rb") as f:
        propio = client.post("/api/informes", files={"archivo": ("mio.pdf", f, "application/pdf")}).json()

    intruso = _registrar(client, "intruso@informeclaro.co")
    assert client.get("/api/informes", headers=intruso).json() == []
    assert client.get(f"/api/informes/{propio['id']}", headers=intruso).status_code == 404
    assert client.delete(f"/api/informes/{propio['id']}", headers=intruso).status_code == 404
    assert client.get(f"/api/informes/{propio['id']}").status_code == 200


def test_preguntas_del_modo_demo_concuerdan_en_numero_y_conservan_siglas():
    from app.services.prompt_orchestrator import _pregunta_fuera_de_rango
    assert _pregunta_fuera_de_rango("Triglicéridos", "alto") == \
        "¿Qué significa para mí que mis triglicéridos estén por encima del rango de referencia?"
    assert _pregunta_fuera_de_rango("Vitamina D", "bajo") == \
        "¿Qué significa para mí que mi vitamina D esté por debajo del rango de referencia?"
    assert "mi colesterol HDL esté" in _pregunta_fuera_de_rango("Colesterol HDL", "bajo")


# --- Idiomas ---------------------------------------------------------------

@pytest.mark.parametrize("cabecera,esperado", [
    (None, "es"),
    ("en-US,en;q=0.9", "en"),
    ("es-CO,es;q=0.9,en;q=0.8", "es"),
    ("fr-FR,en;q=0.5", "en"),
    ("de-DE", "es"),
    ("es;q=0.4,en;q=0.9", "en"),
])
def test_normalizar_idioma(cabecera, esperado):
    from app.i18n import normalizar_idioma
    assert normalizar_idioma(cabecera) == esperado


def test_mensajes_traducidos():
    from app.i18n import t
    assert t("pdf_grande", "en", mb=10) == "The PDF is larger than 10 MB"
    assert t("pdf_grande", "es", mb=10) == "El PDF supera 10 MB"
    assert t("credenciales", "fr") == "Correo o contraseña incorrectos"


def test_idioma_por_defecto_configurable(monkeypatch):
    from app.config import Settings, settings
    from app.i18n import normalizar_idioma
    monkeypatch.setattr(settings, "idioma_por_defecto", "en")
    assert normalizar_idioma(None) == "en"
    assert normalizar_idioma("de-DE") == "en"
    with pytest.raises(ValueError):
        Settings(idioma_por_defecto="fr")


def test_errores_de_informes_en_ingles(client):
    en = {"Accept-Language": "en-US,en;q=0.9"}
    r = client.post("/api/informes", files={"archivo": ("notes.txt", b"hi", "text/plain")}, headers=en)
    assert r.status_code == 415 and r.json()["detail"] == "Only PDF files are accepted"
    r = client.post("/api/informes", files={"archivo": ("roto.pdf", b"no es pdf", "application/pdf")}, headers=en)
    assert r.status_code == 422 and r.json()["detail"] == "The PDF could not be read. Is it a valid file?"
    assert client.get("/api/informes/99999", headers=en).json()["detail"] == "Report not found"
    assert client.get("/api/informes/99999").json()["detail"] == "Informe no encontrado"


def test_errores_de_autenticacion_en_ingles(client):
    en = {"Accept-Language": "en", "Authorization": ""}
    assert client.get("/api/informes", headers=en).json()["detail"] == "Sign in to continue"
    r = client.post("/api/auth/login", json={"email": "nadie@informeclaro.co", "password": "x"}, headers=en)
    assert r.status_code == 401 and r.json()["detail"] == "Incorrect email or password"
    datos = {"nombre": "Paciente Prueba", "email": "prueba@informeclaro.co", "password": "clave-segura-1"}
    r = client.post("/api/auth/registro", json=datos, headers=en)
    assert r.status_code == 409 and r.json()["detail"] == "An account with that email already exists"


def test_migracion_agrega_la_columna_idioma(tmp_path, monkeypatch):
    import sqlite3
    from sqlalchemy import create_engine, inspect
    import app.database as database
    ruta = tmp_path / "vieja.db"
    con = sqlite3.connect(ruta)
    con.execute("CREATE TABLE usuario (id INTEGER PRIMARY KEY, nombre TEXT, email TEXT)")
    con.execute("CREATE TABLE informe_cargado (id INTEGER PRIMARY KEY, usuario_id INTEGER, nombre_archivo TEXT)")
    con.execute("INSERT INTO informe_cargado (usuario_id, nombre_archivo) VALUES (1, 'viejo.pdf')")
    con.commit(); con.close()
    motor = create_engine(f"sqlite:///{ruta}")
    monkeypatch.setattr(database, "engine", motor)
    database.migrar_columnas_nuevas()
    columnas = {c["name"] for c in inspect(motor).get_columns("informe_cargado")}
    assert "idioma" in columnas
    with motor.connect() as conn:
        assert conn.exec_driver_sql("SELECT idioma FROM informe_cargado").scalar() == "es"
    motor.dispose()


def test_el_informe_guarda_el_idioma_pedido(client):
    with PDF_EJEMPLO.open("rb") as f:
        r = client.post("/api/informes", data={"idioma": "en"},
                        files={"archivo": ("informe_ejemplo.pdf", f, "application/pdf")})
    assert r.status_code == 201 and r.json()["idioma"] == "en"
    assert r.json()["aviso"].startswith("This explanation is for information only")
    with PDF_EJEMPLO.open("rb") as f:
        r = client.post("/api/informes", headers={"Accept-Language": "en-GB"},
                        files={"archivo": ("informe_ejemplo.pdf", f, "application/pdf")})
    assert r.json()["idioma"] == "en"


@pytest.mark.parametrize("nombre,idioma,fragmento", [
    ("Glucosa en ayunas", "es", "azúcar en la sangre"),
    ("Glucosa en ayunas", "en", "sugar in your blood"),
    ("Fasting glucose", "en", "sugar in your blood"),
    ("Hemoglobina glicosilada", "en", "two to three months"),
    ("Hemoglobina", "en", "carries oxygen"),
    ("Vitamina D", "es", "calcio"),
    ("Platelet count", "en", "clot"),
])
def test_glosario_bilingue(nombre, idioma, fragmento):
    from app.services.prompt_orchestrator import _que_mide
    assert fragmento in _que_mide(nombre, idioma)


def test_glosario_sin_coincidencia():
    from app.services.prompt_orchestrator import _que_mide
    assert _que_mide("Ferritina", "en") is None


def test_modo_demo_en_ingles(client):
    with PDF_EJEMPLO.open("rb") as f:
        r = client.post("/api/informes", data={"idioma": "en"},
                        files={"archivo": ("informe_ejemplo.pdf", f, "application/pdf")})
    informe = r.json()
    glucosa = next(v for v in informe["valores"] if v["nombre_valor"] == "Glucosa en ayunas")
    assert glucosa["explicacion_ia"].startswith("This test measures the amount of sugar")
    assert "above the laboratory's reference range" in glucosa["explicacion_ia"]
    assert informe["resumen"].startswith("Your report has 12 values; 4 are outside")
    preguntas = [p["texto_pregunta"] for p in informe["preguntas"]]
    assert "What does it mean for me that my hemoglobina is below the reference range?" in preguntas
    assert "How do these results compare with my previous tests?" in preguntas


def test_el_llm_recibe_la_instruccion_de_idioma(monkeypatch):
    from types import SimpleNamespace
    import app.services.prompt_orchestrator as po
    llamadas = []

    class ClienteFalso:
        def __init__(self, **_):
            self.messages = SimpleNamespace(parse=self.parse)

        def parse(self, **kwargs):
            llamadas.append(kwargs)
            salida = po.ResultadoIA(
                resumen="Your report has one value.",
                valores=[po.ValorIA(nombre_valor="Glucose", valor="90", unidad="mg/dL",
                                    rango_referencia="70 - 100", explicacion="This test measures sugar.")],
                preguntas=["How do these results compare with my previous tests?"],
            )
            return SimpleNamespace(stop_reason="end_turn", parsed_output=salida)

    monkeypatch.setattr(po.settings, "anthropic_api_key", "clave-de-prueba")
    monkeypatch.setattr(po.anthropic, "Anthropic", ClienteFalso)
    resultado = po.interpretar("Glucose 90 mg/dL 70 - 100", "en")
    assert "plain English" in llamadas[0]["messages"][0]["content"]
    assert resultado.valores[0].estado == "normal"
    assert resultado.motor == po.settings.llm_model


@pytest.mark.parametrize("texto", [
    "You have diabetes.",
    "This means you have anemia.",
    "You may have a thyroid problem.",
    "Take your medication twice a day.",
    "I recommend a lower dose.",
    "This value is dangerous.",
])
def test_guardrails_en_ingles_bloquean(texto):
    salida = aplicar_guardrails(texto, "en")
    assert salida != texto and salida.startswith("This test is part of your report")


def test_guardrails_en_ingles_dejan_pasar_explicaciones_neutrales():
    texto = "This test measures sugar in your blood. Your result is above the reference range."
    assert aplicar_guardrails(texto, "en") == texto


def test_anonimizar_informe_en_ingles():
    texto = anonimizar(
        "Patient name: John Smith\nDate of birth: 03/14/1961\nMRN: AB-778\n"
        "Address: 12 Oak Street\nFasting glucose 112 mg/dL 70 - 100"
    )
    for dato in ("John Smith", "03/14/1961", "AB-778", "Oak Street"):
        assert dato not in texto
    assert "Fasting glucose 112 mg/dL 70 - 100" in texto


def test_el_ejemplo_en_ingles_se_extrae_y_se_anonimiza():
    from app.services.pdf_parser import extraer_texto, extraer_valores_por_reglas
    ruta = PDF_EJEMPLO.parent / "lab_report_example.pdf"
    texto = anonimizar(extraer_texto(ruta.read_bytes()))
    assert "Jane Doe" not in texto and "4471235" not in texto
    valores = {v.nombre_valor: clasificar(v.valor, v.rango_referencia) for v in extraer_valores_por_reglas(texto)}
    assert len(valores) == 10
    assert valores["Total cholesterol"] == "alto" and valores["Platelet count"] == "bajo"


@pytest.mark.parametrize("idioma,inicio,pregunta", [
    ("en", "This test measures your total cholesterol",
     "What does it mean for me that my total cholesterol is above the reference range?"),
    ("es", "Este examen mide el total de colesterol",
     "¿Qué significa para mí que mi total cholesterol esté por encima del rango de referencia?"),
])
def test_flujo_completo_con_informe_en_ingles(client, idioma, inicio, pregunta):
    ruta = PDF_EJEMPLO.parent / "lab_report_example.pdf"
    with ruta.open("rb") as f:
        r = client.post("/api/informes", data={"idioma": idioma},
                        files={"archivo": ("lab_report_example.pdf", f, "application/pdf")})
    assert r.status_code == 201, r.text
    informe = r.json()
    assert informe["idioma"] == idioma and len(informe["valores"]) == 10
    colesterol = next(v for v in informe["valores"] if v["nombre_valor"] == "Total cholesterol")
    assert colesterol["estado"] == "alto" and colesterol["explicacion_ia"].startswith(inicio)
    assert pregunta in [p["texto_pregunta"] for p in informe["preguntas"]]
    assert client.delete(f"/api/informes/{informe['id']}").status_code == 204
