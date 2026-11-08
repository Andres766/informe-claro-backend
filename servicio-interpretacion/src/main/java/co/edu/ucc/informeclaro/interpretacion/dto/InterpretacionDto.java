package co.edu.ucc.informeclaro.interpretacion.dto;

/** Resultado completo de la interpretación. */
public record InterpretacionDto(String resumen, java.util.List<ValorInterpretadoDto> valores, java.util.List<String> preguntas, String motor) {
}
