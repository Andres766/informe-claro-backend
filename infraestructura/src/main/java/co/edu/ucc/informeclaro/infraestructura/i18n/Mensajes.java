package co.edu.ucc.informeclaro.infraestructura.i18n;

import java.util.Map;

/** Mensajes de la API en español e inglés. Los microservicios envían la clave; aquí se traduce. */
public final class Mensajes {

    private static final Map<String, Map<String, String>> TEXTOS = Map.ofEntries(
            Map.entry("solo_pdf", Map.of("es", "Solo se aceptan archivos PDF", "en", "Only PDF files are accepted")),
            Map.entry("pdf_grande", Map.of("es", "El PDF supera %s MB", "en", "The PDF is larger than %s MB")),
            Map.entry("pdf_ilegible", Map.of("es", "No se pudo leer el PDF. ¿Es un archivo válido?",
                    "en", "The PDF could not be read. Is it a valid file?")),
            Map.entry("pdf_sin_texto", Map.of("es", "El PDF no contiene texto seleccionable (posiblemente es una imagen escaneada).",
                    "en", "The PDF has no selectable text (it is probably a scanned image).")),
            Map.entry("sin_valores", Map.of("es", "No se encontraron valores de laboratorio en el PDF",
                    "en", "No laboratory values were found in the PDF")),
            Map.entry("informe_no_encontrado", Map.of("es", "Informe no encontrado", "en", "Report not found")),
            Map.entry("inicia_sesion", Map.of("es", "Inicia sesión para continuar", "en", "Sign in to continue")),
            Map.entry("correo_repetido", Map.of("es", "Ya existe una cuenta con ese correo",
                    "en", "An account with that email already exists")),
            Map.entry("credenciales", Map.of("es", "Correo o contraseña incorrectos", "en", "Incorrect email or password")),
            Map.entry("idioma_no_soportado", Map.of("es", "Idioma no soportado. Usa «es» o «en».",
                    "en", "Unsupported language. Use “es” or “en”.")),
            Map.entry("servicio_no_disponible", Map.of("es", "Un servicio interno no está disponible. Intenta de nuevo en un momento.",
                    "en", "An internal service is unavailable. Please try again in a moment.")),
            Map.entry("error_interno", Map.of("es", "Ocurrió un error inesperado.", "en", "An unexpected error occurred.")),
            Map.entry("archivo_requerido", Map.of("es", "Falta el archivo PDF", "en", "The PDF file is missing")));

    private Mensajes() {
    }

    public static String t(String clave, String idioma, Object... datos) {
        Map<String, String> textos = TEXTOS.getOrDefault(clave, TEXTOS.get("error_interno"));
        String texto = textos.getOrDefault(idioma, textos.get("es"));
        return datos.length == 0 ? texto : texto.formatted(datos);
    }
}
