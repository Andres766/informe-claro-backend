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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cliente REST de la API de Gemini (Google): una API de TERCEROS, por eso se usa REST y no gRPC.
 * Pide la respuesta en JSON validado contra un esquema para recibir siempre el mismo formato.
 * Si Google responde saturado (503) o sin cuota por minuto (429), reintenta y luego prueba el
 * modelo de respaldo antes de rendirse (entonces el sistema usa el motor por reglas). Si un modelo
 * tarda demasiado, pasa al de respaldo; todo dentro de un presupuesto de tiempo para no dejar al
 * paciente esperando.
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
    private final long presupuestoMs;
    private final int presupuestoPensamiento;
    // Se apaga si la API rechaza thinkingConfig para el modelo configurado.
    private final AtomicBoolean enviarPensamiento;

    public GeminiRestCliente(RestClient restClientGemini, JsonMapper json,
                             @Value("${informeclaro.gemini.api-key:}") String apiKey,
                             @Value("${informeclaro.gemini.modelo}") String modelo,
                             @Value("${informeclaro.gemini.modelo-respaldo:}") String modeloRespaldo,
                             @Value("${informeclaro.gemini.espera-reintento-ms:3000}") long esperaMs,
                             @Value("${informeclaro.gemini.presupuesto-s:150}") long presupuestoS,
                             @Value("${informeclaro.gemini.pensamiento:0}") int presupuestoPensamiento) {
        this.http = restClientGemini;
        this.json = json;
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.modeloRespaldo = modeloRespaldo;
        this.esperaMs = esperaMs;
        this.presupuestoMs = presupuestoS * 1000;
        this.presupuestoPensamiento = presupuestoPensamiento;
        this.enviarPensamiento = new AtomicBoolean(presupuestoPensamiento >= 0);
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
    public RespuestaLlm interpretar(String instruccionSistema, String mensajeUsuario) {
        List<String> modelos = new ArrayList<>(List.of(modelo));
        if (modeloRespaldo != null && !modeloRespaldo.isBlank() && !modeloRespaldo.equals(modelo)) {
            modelos.add(modeloRespaldo);
        }
        long inicio = System.currentTimeMillis();
        List<String> historial = new ArrayList<>(); // qué pasó en cada intento, para la bitácora
        LlmNoDisponibleException ultimo = null;
        for (String m : modelos) {
            for (int intento = 1; intento <= INTENTOS_POR_MODELO; intento++) {
                if (ultimo != null && System.currentTimeMillis() - inicio >= presupuestoMs) {
                    historial.add("se agotó el tiempo");
                    throw new LlmNoDisponibleException(String.join(" | ", historial), ultimo);
                }
                try {
                    return new RespuestaLlm(llamarConPensamientoOpcional(m, instruccionSistema, mensajeUsuario, historial), m);
                } catch (FalloPorTiempoException e) {
                    ultimo = e;
                    historial.add(m + ": " + resumir(e));
                    log.warn("Gemini {} tardó demasiado, se prueba el siguiente modelo", m);
                    break;
                } catch (FalloPasajeroException e) {
                    ultimo = e;
                    historial.add(m + ": " + resumir(e));
                    log.warn("Gemini {} no disponible (intento {} de {}): {}", m, intento, INTENTOS_POR_MODELO, e.getMessage());
                    esperar(esperaMs * intento);
                } catch (LlmNoDisponibleException e) {
                    historial.add(m + ": " + resumir(e));
                    throw new LlmNoDisponibleException(String.join(" | ", historial), e);
                }
            }
        }
        throw new LlmNoDisponibleException(String.join(" | ", historial), ultimo);
    }

    private static String resumir(Exception e) {
        String mensaje = String.valueOf(e.getMessage()).replaceAll("\\s+", " ");
        return mensaje.length() <= 200 ? mensaje : mensaje.substring(0, 200) + "…";
    }

    /** Pide no "pensar" (más rápido; explicar valores no lo necesita). Si el modelo no lo acepta, repite sin ese ajuste. */
    private ResultadoLlmDto llamarConPensamientoOpcional(String modelo, String instruccionSistema, String mensajeUsuario,
                                                         List<String> historial) {
        boolean conPensamiento = enviarPensamiento.get();
        try {
            return llamar(modelo, instruccionSistema, mensajeUsuario, conPensamiento);
        } catch (PeticionRechazadaException e) {
            if (!conPensamiento) {
                throw e;
            }
            enviarPensamiento.set(false);
            historial.add(modelo + ": rechazó thinkingConfig (" + resumir(e) + ")");
            log.warn("Gemini {} rechazó thinkingConfig; se continúa sin él: {}", modelo, e.getMessage());
            return llamar(modelo, instruccionSistema, mensajeUsuario, false);
        }
    }

    /** POST /v1beta/models/{modelo}:generateContent con la instrucción de sistema y el informe. */
    @SuppressWarnings("unchecked")
    private ResultadoLlmDto llamar(String modelo, String instruccionSistema, String mensajeUsuario, boolean conPensamiento) {
        Map<String, Object> configuracion = new HashMap<>(Map.of(
                "responseMimeType", "application/json",
                "responseJsonSchema", EsquemaSalida.json(false)));
        if (conPensamiento) {
            configuracion.put("thinkingConfig", Map.of("thinkingBudget", presupuestoPensamiento));
        }
        Map<String, Object> cuerpo = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", instruccionSistema))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", mensajeUsuario)))),
                "generationConfig", configuracion);
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
        } catch (HttpClientErrorException.BadRequest e) {
            throw new PeticionRechazadaException("Gemini rechazó la petición: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new FalloPorTiempoException("Sin respuesta de Gemini a tiempo: " + e.getMessage(), e);
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

    /** El modelo no respondió a tiempo (o se cortó la conexión): mejor probar el siguiente modelo. */
    private static class FalloPorTiempoException extends LlmNoDisponibleException {
        FalloPorTiempoException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }

    /** 400: la API no aceptó algún campo de la petición. */
    private static class PeticionRechazadaException extends LlmNoDisponibleException {
        PeticionRechazadaException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
