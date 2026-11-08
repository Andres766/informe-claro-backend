package co.edu.ucc.informeclaro.documentos.dto;

/** Valor encontrado en el informe con su estado frente al rango. */
public record ValorExtraidoDto(String nombreValor, String valor, String unidad, String rangoReferencia, String estado) {
}
