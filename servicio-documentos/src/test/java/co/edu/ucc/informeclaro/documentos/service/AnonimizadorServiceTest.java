package co.edu.ucc.informeclaro.documentos.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnonimizadorServiceTest {

    private final AnonimizadorService anonimizador = new AnonimizadorService();

    @Test
    void ocultaDatosPersonalesEnEspanol() {
        String texto = anonimizador.anonimizar(
                "Paciente: Juan Pérez\nCorreo juan@mail.com\nGlucosa 90 mg/dL 70 - 100\nID 1085123456");
        assertThat(texto).doesNotContain("Juan", "juan@mail.com", "1085123456");
        assertThat(texto).contains("Glucosa 90 mg/dL 70 - 100");
    }

    @Test
    void ocultaDatosPersonalesEnIngles() {
        String texto = anonimizador.anonimizar("Patient name: John Smith\nDate of birth: 03/14/1961\nMRN: AB-778\n"
                + "Address: 12 Oak Street\nFasting glucose 112 mg/dL 70 - 100");
        assertThat(texto).doesNotContain("John Smith", "03/14/1961", "AB-778", "Oak Street");
        assertThat(texto).contains("Fasting glucose 112 mg/dL 70 - 100");
    }
}
