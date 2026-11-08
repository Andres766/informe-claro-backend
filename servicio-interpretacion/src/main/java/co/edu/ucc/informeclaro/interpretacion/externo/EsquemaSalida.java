package co.edu.ucc.informeclaro.interpretacion.externo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON Schema de la salida estructurada que se pide a cualquier proveedor de IA (ver ResultadoLlmDto). */
final class EsquemaSalida {

    private EsquemaSalida() {
    }

    /** @param cerrado agrega additionalProperties=false (Anthropic lo exige; Gemini no lo necesita). */
    static Map<String, Object> json(boolean cerrado) {
        Map<String, Object> valor = objeto(Map.of(
                "nombre_valor", texto("Nombre del examen tal como aparece en el informe"),
                "valor", texto("Resultado numérico o textual, sin la unidad"),
                "unidad", texto("Unidad de medida; vacío si no aplica"),
                "rango_referencia", texto("Rango impreso, p. ej. '70 - 100' o '< 200'; vacío si no aparece"),
                "explicacion", texto("Explicación en lenguaje sencillo, sin diagnosticar")),
                List.of("nombre_valor", "valor", "unidad", "rango_referencia", "explicacion"), cerrado);
        return objeto(Map.of(
                "resumen", texto("2 a 3 frases que resumen el informe sin diagnosticar"),
                "valores", Map.of("type", "array", "items", valor),
                "preguntas", Map.of("type", "array", "items", Map.of("type", "string"),
                        "description", "Entre 3 y 6 preguntas para el médico")),
                List.of("resumen", "valores", "preguntas"), cerrado);
    }

    private static Map<String, Object> objeto(Map<String, Object> propiedades, List<String> requeridas, boolean cerrado) {
        Map<String, Object> esquema = new LinkedHashMap<>();
        esquema.put("type", "object");
        esquema.put("properties", propiedades);
        esquema.put("required", requeridas);
        if (cerrado) {
            esquema.put("additionalProperties", false);
        }
        return esquema;
    }

    private static Map<String, Object> texto(String descripcion) {
        return Map.of("type", "string", "description", descripcion);
    }
}
