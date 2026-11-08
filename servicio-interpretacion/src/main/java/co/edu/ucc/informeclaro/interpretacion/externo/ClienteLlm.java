package co.edu.ucc.informeclaro.interpretacion.externo;

import co.edu.ucc.informeclaro.interpretacion.dto.ResultadoLlmDto;

/**
 * Proveedor de IA (API de terceros, siempre por REST). Hay dos implementaciones: Gemini (Google) y
 * Claude (Anthropic); se usa la que tenga API key configurada.
 */
public interface ClienteLlm {

    boolean configurado();

    String modelo();

    /** Envía la instrucción de sistema y el informe; devuelve la salida estructurada. */
    ResultadoLlmDto interpretar(String instruccionSistema, String mensajeUsuario);
}
