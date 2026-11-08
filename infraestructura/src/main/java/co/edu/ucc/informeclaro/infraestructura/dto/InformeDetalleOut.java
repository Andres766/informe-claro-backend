package co.edu.ucc.informeclaro.infraestructura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Informe completo, con el mismo formato JSON que usa el frontend. */
public record InformeDetalleOut(
        long id,
        @JsonProperty("nombre_archivo") String nombreArchivo,
        @JsonProperty("fecha_carga") String fechaCarga,
        String motor,
        String idioma,
        String resumen,
        List<ValorOut> valores,
        List<PreguntaOut> preguntas,
        String aviso) {
}
