package co.edu.ucc.informeclaro.informes.dto;

/** Fila del historial. */
public record InformeResumenDto(long id, String nombreArchivo, java.time.Instant fechaCarga, String motor, String idioma, int totalValores, int fueraDeRango) {
}
