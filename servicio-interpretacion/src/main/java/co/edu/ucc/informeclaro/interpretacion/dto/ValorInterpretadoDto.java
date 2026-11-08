package co.edu.ucc.informeclaro.interpretacion.dto;

/** Valor con su explicación en lenguaje sencillo. */
public record ValorInterpretadoDto(String nombreValor, String valor, String unidad, String rangoReferencia, String estado, String explicacion) {
}
