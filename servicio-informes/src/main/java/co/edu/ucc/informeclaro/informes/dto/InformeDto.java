package co.edu.ucc.informeclaro.informes.dto;

/** Informe completo con valores y preguntas. */
public record InformeDto(long id, String nombreArchivo, java.time.Instant fechaCarga, String motor, String idioma, String resumen, java.util.List<ValorDto> valores, java.util.List<PreguntaDto> preguntas) {
}
