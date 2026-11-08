package co.edu.ucc.informeclaro.informes.dto;

/** Valor de un informe. */
public record ValorDto(Long id, String nombreValor, String unidad, String valor, String rangoReferencia, String estado, String explicacionIa) {
}
