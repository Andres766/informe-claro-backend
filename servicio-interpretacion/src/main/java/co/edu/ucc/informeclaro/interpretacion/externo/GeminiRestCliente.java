package co.edu.ucc.informeclaro.interpretacion.externo;

import co.edu.ucc.informeclaro.interpretacion.dto.ResultadoLlmDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * Cliente REST de la API de Gemini (Google): una API de TERCEROS, por eso se usa REST y no gRPC.
 * Pide la respuesta en JSON validado contra un esquema para recibir siempre el mismo formato.
 */
@Component
public class GeminiRestCliente implements ClienteLlm {

    private final RestClient http;
    private final JsonMapper json;
    private final String apiKey;
    private final String modelo;

    public GeminiRestCliente(RestClient restClientGemini, JsonMapper json,
                             @Value("${informeclaro.gemini.api-key:}") String apiKey,
                             @Value("${informeclaro.gemini.modelo}") String modelo) {
        this.http = restClientGemini;
        this.json = json;
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    @Override
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String modelo() {
        return modelo;
    }

    /** POST /v1beta/models/{modelo}:generateContent con la instrucción de sistema y el informe. */
    @Override
    @SuppressWarnings("unchecked")
    public ResultadoLlmDto interpretar(String instruccionSistema, String mensajeUsuario) {
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
}
