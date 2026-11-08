package co.edu.ucc.informeclaro.interpretacion.externo;

import co.edu.ucc.informeclaro.interpretacion.dto.ResultadoLlmDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cliente REST de la API de Gemini (Google): una API de TERCEROS, por eso se usa REST y no gRPC.
 * Pide la respuesta en JSON validado contra un esquema para recibir siempre el mismo formato.
 * Si Google responde saturado (503) o sin cuota por minuto (429), reintenta y luego prueba el
 * modelo de respaldo antes de rendirse (entonces el sistema usa el motor por reglas).
 */
@Component
public class GeminiRestCliente implements ClienteLlm {

    private static final Logger log = LoggerFactory.getLogger(GeminiRestCliente.class);
    private static final int INTENTOS_POR_MODELO = 2;

    private final RestClient http;
    private final JsonMapper json;
    private final String apiKey;
    private final String modelo;
    private final String modeloRespaldo;
    private final long esperaMs;

    public GeminiRestCliente(RestClient restClientGemini, JsonMapper json,
                             @Value("${informeclaro.gemini.api-key:}") String apiKey,
                             @Value("${informeclaro.gemini.modelo}") String modelo,
                             @Value("${informeclaro.gemini.modelo-respaldo:}") String modeloRespaldo,
                             @Value("${informeclaro.gemini.espera-reintento-ms:3000}") long esperaMs) {
        this.http = restClientGemini;
        this.json = json;
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.modeloRespaldo = modeloRespaldo;
        this.esperaMs = esperaMs;
    }

    @Override
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String modelo() {
        return modelo;
    }

    /** Prueba el modelo principal y luego el de respaldo, reintentando solo los fallos pasajeros. */
    @Override
    public ResultadoLlmDto interpretar(String instruccionSistema, String mensajeUsuario) {
        List<String> modelos = new ArrayList<>(List.of(modelo));
        if (modeloRespaldo != null && !modeloRespaldo.isBlank() && !modeloRespaldo.equals(modelo)) {
            modelos.add(modeloRespaldo);
        }
        LlmNoDisponibleException ultimo = null;
        for (String m : modelos) {
            for (int intento = 1; intento <= INTENTOS_POR_MODELO; intento++) {
                try {
                    return llamar(m, instruccionSistema, mensajeUsuario);
                } catch (FalloPasajeroException e) {
                    ultimo = e;
                    log.warn("Gemini {} no disponible (intento {} de {}): {}", m, intento, INTENTOS_POR_MODELO, e.getMessage());
                    esperar(esperaMs * intento);
                }
            }
        }
        throw ultimo;
    }

    /** POST /v1beta/models/{modelo}:generateContent con la instrucción de sistema y el informe. */
    @SuppressWarnings("unchecked")
    private ResultadoLlmDto llamar(String modelo, String instruccionSistema, String mensajeUsuario) {
        Map<String, Object> cuerpo = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", instruccionSistema))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", mensajeUsuario)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", EsquemaSalida.json(false)));
        Map<String, Object> respuesta;
        try {
            respuesta = http.post()
                    .uri("/v1beta/models/{modelo}:generateContent", modelo)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests e) {
            throw new FalloPasajeroException("Fallo al llamar a la API de Gemini: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new LlmNoDisponibleException("Fallo al llamar a la API de Gemini: " + e.getMessage(), e);
        }
        if (respuesta == null) {
            throw new LlmNoDisponibleException("La API de Gemini respondió vacío", null);
        }
        List<Map<String, Object>> candidatos = (List<Map<String, Object>>) respuesta.getOrDefault("candidates", List.of());
        if (candidatos.isEmpty()) {
            // Sin candidatos: Gemini bloqueó la petición (promptFeedback.blockReason).
            throw new LlmNoDisponibleException("Gemini no devolvió respuesta: " + respuesta.get("promptFeedback"), null);
        }
        Map<String, Object> candidato = candidatos.get(0);
        Object razon = candidato.get("finishReason");
        if (!"STOP".equals(razon)) {
            throw new LlmNoDisponibleException("Gemini terminó sin completar la respuesta (finishReason=" + razon + ")", null);
        }
        Map<String, Object> contenido = (Map<String, Object>) candidato.getOrDefault("content", Map.of());
        // Las partes de razonamiento (thought=true) se ignoran; el JSON viene en la parte de texto.
        String texto = ((List<Map<String, Object>>) contenido.getOrDefault("parts", List.of())).stream()
                .filter(parte -> !Boolean.TRUE.equals(parte.get("thought")) && parte.get("text") != null)
                .map(parte -> (String) parte.get("text"))
                .findFirst()
                .orElseThrow(() -> new LlmNoDisponibleException("Respuesta de Gemini sin texto", null));
        try {
            return json.readValue(texto, ResultadoLlmDto.class);
        } catch (RuntimeException e) {
            throw new LlmNoDisponibleException("La salida del modelo no tiene el formato esperado", e);
        }
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Saturación o límite por minuto: vale la pena reintentar. */
    private static class FalloPasajeroException extends LlmNoDisponibleException {
        FalloPasajeroException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
