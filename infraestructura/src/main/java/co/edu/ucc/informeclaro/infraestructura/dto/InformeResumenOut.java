package co.edu.ucc.informeclaro.infraestructura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Fila del historial. */
public record InformeResumenOut(
        long id,
        @JsonProperty("nombre_archivo") String nombreArchivo,
        @JsonProperty("fecha_carga") String fechaCarga,
        String motor,
        String idioma,
        @JsonProperty("total_valores") int totalValores,
        @JsonProperty("fuera_de_rango") int fueraDeRango) {
}
