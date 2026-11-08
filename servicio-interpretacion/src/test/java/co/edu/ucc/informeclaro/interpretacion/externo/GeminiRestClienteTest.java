package co.edu.ucc.informeclaro.interpretacion.externo;

import co.edu.ucc.informeclaro.interpretacion.DocumentosFalso;
import co.edu.ucc.informeclaro.interpretacion.dto.InterpretacionDto;
import co.edu.ucc.informeclaro.interpretacion.service.GuardrailsService;
import co.edu.ucc.informeclaro.interpretacion.service.MotorLlmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Verifica la llamada REST a la API de terceros (Gemini) sin salir a internet. */
class GeminiRestClienteTest {

    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";
    private static final String URL_RESPALDO = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent";
    private static final String RESPUESTA_CORTA = """
            {"candidates":[{"finishReason":"STOP","content":{"parts":[
              {"text":"{\\"resumen\\":\\"Listo.\\",\\"valores\\":[],\\"preguntas\\":[]}"}]}}]}""";

    private MockRestServiceServer api;
    private GeminiRestCliente cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        api = MockRestServiceServer.bindTo(constructor).build();
        cliente = new GeminiRestCliente(constructor.build(), JsonMapper.builder().build(), "clave-de-prueba",
                "gemini-flash-latest", "gemini-flash-lite-latest", 0);
    }

    @Test
    void enviaLaPeticionRestConSalidaJsonYAplicaGuardrails() {
        api.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "clave-de-prueba"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value(org.hamcrest.Matchers.containsString("InformeClaro")))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseJsonSchema.required[0]").value("resumen"))
                .andExpect(jsonPath("$.generationConfig.responseJsonSchema.additionalProperties").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("plain English")))
                .andRespond(withSuccess("""
                        {"candidates":[{"finishReason":"STOP","content":{"role":"model","parts":[
                          {"text":"Pensando...","thought":true},
                          {"text":"{\\"resumen\\":\\"Your report has two values.\\",\\"valores\\":[{\\"nombre_valor\\":\\"Glucose\\",\\"valor\\":\\"90\\",\\"unidad\\":\\"mg/dL\\",\\"rango_referencia\\":\\"70 - 100\\",\\"explicacion\\":\\"This test measures sugar.\\"},{\\"nombre_valor\\":\\"TSH\\",\\"valor\\":\\"9\\",\\"unidad\\":\\"uUI/mL\\",\\"rango_referencia\\":\\"\\",\\"explicacion\\":\\"You have a thyroid disease.\\"}],\\"preguntas\\":[\\"How do these results compare with my previous tests?\\",\\"I recommend a lower dose?\\"]}"}
                        ]}}]}""", MediaType.APPLICATION_JSON));

        MotorLlmService motor = new MotorLlmService(cliente, new DocumentosFalso(), new GuardrailsService());
        MotorLlmService.Resultado r = motor.interpretar("Glucose 90 mg/dL 70 - 100", "en");
        InterpretacionDto i = r.interpretacion();

        api.verify();
        assertThat(i.motor()).isEqualTo("gemini-flash-latest");
        assertThat(i.valores()).extracting(v -> v.estado()).containsExactly("normal", "sin_rango");
        assertThat(i.valores().get(1).explicacion()).startsWith("This test is part of your report");
        assertThat(i.preguntas()).containsExactly("How do these results compare with my previous tests?");
        assertThat(r.guardrailsActivados()).isEqualTo(1);
    }

    @Test
    void unBloqueoDeSeguridadEsUnFalloControlado() {
        api.expect(requestTo(URL))
                .andRespond(withSuccess("{\"candidates\":[{\"finishReason\":\"SAFETY\",\"content\":{\"parts\":[]}}]}",
                        MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> cliente.interpretar("sistema", "usuario"))
                .isInstanceOf(LlmNoDisponibleException.class);
    }

    @Test
    void siEstaSaturadoReintentaYLuegoUsaElModeloDeRespaldo() {
        api.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        api.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        api.expect(requestTo(URL_RESPALDO)).andRespond(withSuccess(RESPUESTA_CORTA, MediaType.APPLICATION_JSON));

        assertThat(cliente.interpretar("sistema", "usuario").resumen()).isEqualTo("Listo.");
        api.verify();
    }

    @Test
    void siTodosLosModelosEstanSaturadosEsUnFalloControlado() {
        for (String url : new String[] {URL, URL, URL_RESPALDO, URL_RESPALDO}) {
            api.expect(requestTo(url)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }
        assertThatThrownBy(() -> cliente.interpretar("sistema", "usuario"))
                .isInstanceOf(LlmNoDisponibleException.class);
        api.verify();
    }

    @Test
    void unaPeticionInvalidaNoSeReintenta() {
        api.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> cliente.interpretar("sistema", "usuario"))
                .isInstanceOf(LlmNoDisponibleException.class);
        api.verify();
    }
}
