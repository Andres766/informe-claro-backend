package co.edu.ucc.informeclaro.informes.dto;

/** Datos para guardar un informe ya interpretado. */
public record NuevoInformeDto(long usuarioId, String nombreArchivo, String resumen, String motor, String idioma, java.util.List<ValorDto> valores, java.util.List<String> preguntas) {
}
