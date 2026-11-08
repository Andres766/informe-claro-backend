package co.edu.ucc.informeclaro.interpretacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Salida estructurada que devuelve la IA, Gemini o Claude (validada contra un JSON Schema). */
public record ResultadoLlmDto(String resumen, List<Valor> valores, List<String> preguntas) {

    public record Valor(
            @JsonProperty("nombre_valor") String nombreValor,
            String valor,
            String unidad,
            @JsonProperty("rango_referencia") String rangoReferencia,
            String explicacion) {
    }
}
