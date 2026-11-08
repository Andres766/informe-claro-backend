package co.edu.ucc.informeclaro.interpretacion.dto;

/** Valor extraído del informe, con su estado ya calculado por el microservicio de documentos. */
public record ValorBaseDto(String nombreValor, String valor, String unidad, String rangoReferencia, String estado) {
}
