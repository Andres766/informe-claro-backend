package co.edu.ucc.informeclaro.interpretacion.externo;

import co.edu.ucc.informeclaro.interpretacion.dto.ResultadoLlmDto;

/** Salida del proveedor de IA y el modelo que la generó (puede ser el de respaldo). */
public record RespuestaLlm(ResultadoLlmDto resultado, String modelo) {
}
