"""Idiomas de la API: español (por defecto) e inglés.

El idioma de cada petición sale del encabezado Accept-Language que envía el frontend.
"""
from .config import settings

IDIOMAS = ("es", "en")
IDIOMA_POR_DEFECTO = "es"  # textos de respaldo si falta una traducción

MENSAJES = {
    "solo_pdf": {
        "es": "Solo se aceptan archivos PDF",
        "en": "Only PDF files are accepted",
    },
    "pdf_grande": {
        "es": "El PDF supera {mb} MB",
        "en": "The PDF is larger than {mb} MB",
    },
    "pdf_ilegible": {
        "es": "No se pudo leer el PDF. ¿Es un archivo válido?",
        "en": "The PDF could not be read. Is it a valid file?",
    },
    "pdf_sin_texto": {
        "es": "El PDF no contiene texto seleccionable (posiblemente es una imagen escaneada).",
        "en": "The PDF has no selectable text (it is probably a scanned image).",
    },
    "sin_valores": {
        "es": "No se encontraron valores de laboratorio en el PDF",
        "en": "No laboratory values were found in the PDF",
    },
    "informe_no_encontrado": {
        "es": "Informe no encontrado",
        "en": "Report not found",
    },
    "inicia_sesion": {
        "es": "Inicia sesión para continuar",
        "en": "Sign in to continue",
    },
    "correo_repetido": {
        "es": "Ya existe una cuenta con ese correo",
        "en": "An account with that email already exists",
    },
    "credenciales": {
        "es": "Correo o contraseña incorrectos",
        "en": "Incorrect email or password",
    },
    "idioma_no_soportado": {
        "es": "Idioma no soportado. Usa «es» o «en».",
        "en": "Unsupported language. Use “es” or “en”.",
    },
}


def normalizar_idioma(valor: str | None, por_defecto: str | None = None) -> str:
    """'en-US,en;q=0.9,es;q=0.8' -> 'en'. Devuelve el primer idioma soportado por orden de preferencia."""
    por_defecto = por_defecto or settings.idioma_por_defecto
    if not valor:
        return por_defecto
    candidatos = []
    for parte in valor.split(","):
        etiqueta, _, parametros = parte.strip().partition(";")
        calidad = 1.0
        if parametros.strip().startswith("q="):
            try:
                calidad = float(parametros.strip()[2:])
            except ValueError:
                calidad = 0.0
        candidatos.append((calidad, etiqueta.strip().lower()[:2]))
    for _, codigo in sorted(candidatos, key=lambda c: -c[0]):
        if codigo in IDIOMAS:
            return codigo
    return por_defecto


def t(clave: str, idioma: str = IDIOMA_POR_DEFECTO, **datos) -> str:
    textos = MENSAJES[clave]
    return textos.get(idioma, textos[IDIOMA_POR_DEFECTO]).format(**datos)
