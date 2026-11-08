package co.edu.ucc.informeclaro.interpretacion.externo;

import co.edu.ucc.informeclaro.interpretacion.DocumentosFalso;
import co.edu.ucc.informeclaro.interpretacion.dto.InterpretacionDto;
import co.edu.ucc.informeclaro.interpretacion.service.GuardrailsService;
import co.edu.ucc.informeclaro.interpretacion.service.MotorClaudeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Verifica la llamada REST a la API de terceros (Anthropic) sin salir a internet. */
class AnthropicRestClienteTest {

    private MockRestServiceServer api;
    private AnthropicRestCliente cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder().baseUrl("https://api.anthropic.com");
        api = MockRestServiceServer.bindTo(constructor).build();
        cliente = new AnthropicRestCliente(constructor.build(), JsonMapper.builder().build(), "clave-de-prueba", "claude-opus-5");
    }

    @Test
    void enviaLaPeticionRestConSalidaEstructuradaYAplicaGuardrails() {
        api.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "clave-de-prueba"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(header("anthropic-beta", "server-side-fallback-2026-07-01"))
                .andExpect(jsonPath("$.model").value("claude-opus-5"))
                .andExpect(jsonPath("$.output_config.format.type").value("json_schema"))
                .andExpect(jsonPath("$.fallbacks").value("default"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("plain English")))
                .andRespond(withSuccess("""
                        {"stop_reason":"end_turn","content":[
                          {"type":"thinking","thinking":""},
                          {"type":"text","text":"{\\"resumen\\":\\"Your report has two values.\\",\\"valores\\":[{\\"nombre_valor\\":\\"Glucose\\",\\"valor\\":\\"90\\",\\"unidad\\":\\"mg/dL\\",\\"rango_referencia\\":\\"70 - 100\\",\\"explicacion\\":\\"This test measures sugar.\\"},{\\"nombre_valor\\":\\"TSH\\",\\"valor\\":\\"9\\",\\"unidad\\":\\"uUI/mL\\",\\"rango_referencia\\":\\"\\",\\"explicacion\\":\\"You have a thyroid disease.\\"}],\\"preguntas\\":[\\"How do these results compare with my previous tests?\\",\\"I recommend a lower dose?\\"]}"}
                        ]}""", MediaType.APPLICATION_JSON));

        MotorClaudeService motor = new MotorClaudeService(cliente, new DocumentosFalso(), new GuardrailsService());
        MotorClaudeService.Resultado r = motor.interpretar("Glucose 90 mg/dL 70 - 100", "en");
        InterpretacionDto i = r.interpretacion();

        api.verify();
        assertThat(i.motor()).isEqualTo("claude-opus-5");
        assertThat(i.valores()).extracting(v -> v.estado()).containsExactly("normal", "sin_rango");
        assertThat(i.valores().get(1).explicacion()).startsWith("This test is part of your report");
        assertThat(i.preguntas()).containsExactly("How do these results compare with my previous tests?");
        assertThat(r.guardrailsActivados()).isEqualTo(1);
    }

    @Test
    void unaNegativaDelModeloEsUnFalloControlado() {
        api.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess("{\"stop_reason\":\"refusal\",\"content\":[]}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> cliente.interpretar("sistema", "usuario"))
                .isInstanceOf(AnthropicRestCliente.ClaudeNoDisponibleException.class);
    }
}
