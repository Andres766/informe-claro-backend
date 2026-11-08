package co.edu.ucc.informeclaro.documentos.dto;

/** Texto crudo extraído del PDF (antes de anonimizar). */
public record TextoPdfDto(String texto, int paginas) {
}
