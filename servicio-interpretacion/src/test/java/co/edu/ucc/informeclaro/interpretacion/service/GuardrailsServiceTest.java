package co.edu.ucc.informeclaro.interpretacion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class GuardrailsServiceTest {

    private final GuardrailsService guardrails = new GuardrailsService();

    @ParameterizedTest
    @ValueSource(strings = {"Usted tiene diabetes.", "Te recomiendo tomar metformina.",
            "Tome el medicamento cada 8 horas.", "Este valor es peligroso."})
    void bloqueaEnEspanol(String texto) {
        GuardrailsService.Revision r = guardrails.revisar(texto, "es");
        assertThat(r.activado()).isTrue();
        assertThat(r.texto()).startsWith("Este examen hace parte de tu informe");
    }

    @ParameterizedTest
    @ValueSource(strings = {"You have diabetes.", "This means you have anemia.", "You may have a thyroid problem.",
            "Take your medication twice a day.", "I recommend a lower dose.", "This value is dangerous."})
    void bloqueaEnIngles(String texto) {
        assertThat(guardrails.aplicar(texto, "en")).startsWith("This test is part of your report");
    }

    @Test
    void dejaPasarExplicacionesNeutrales() {
        String es = "Este examen mide el azúcar en la sangre. Tu resultado está por encima del rango de referencia.";
        String en = "This test measures sugar in your blood. Your result is above the reference range.";
        assertThat(guardrails.aplicar(es, "es")).isEqualTo(es);
        assertThat(guardrails.aplicar(en, "en")).isEqualTo(en);
    }
}
