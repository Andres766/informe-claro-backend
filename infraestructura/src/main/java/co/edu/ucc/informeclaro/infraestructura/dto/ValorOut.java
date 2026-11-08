package co.edu.ucc.informeclaro.infraestructura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ValorOut(
        long id,
        @JsonProperty("nombre_valor") String nombreValor,
        String unidad,
        String valor,
        @JsonProperty("rango_referencia") String rangoReferencia,
        String estado,
        @JsonProperty("explicacion_ia") String explicacionIa) {
}
