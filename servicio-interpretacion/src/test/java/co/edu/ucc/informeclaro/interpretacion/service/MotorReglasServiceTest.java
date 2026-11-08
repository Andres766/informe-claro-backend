package co.edu.ucc.informeclaro.interpretacion.service;

import co.edu.ucc.informeclaro.interpretacion.DocumentosFalso;
import co.edu.ucc.informeclaro.interpretacion.dto.InterpretacionDto;
import co.edu.ucc.informeclaro.interpretacion.dto.ValorInterpretadoDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MotorReglasServiceTest {

    private final MotorReglasService motor = new MotorReglasService(new DocumentosFalso());

    @Test
    void enEspanol() {
        InterpretacionDto r = motor.interpretar("texto", "es", "demo (reglas)");
        assertThat(r.valores()).hasSize(12);
        assertThat(r.resumen()).startsWith("Tu informe tiene 12 valores; 4 están fuera");
        ValorInterpretadoDto glucosa = r.valores().get(0);
        assertThat(glucosa.explicacion()).startsWith("Este examen mide la cantidad de azúcar")
                .contains("por encima del rango de referencia del laboratorio (70 - 100)");
        assertThat(r.preguntas()).contains("¿Qué significa para mí que mi hemoglobina esté por debajo del rango de referencia?",
                "¿Cómo se comparan estos resultados con mis exámenes anteriores?");
    }

    @Test
    void enIngles() {
        InterpretacionDto r = motor.interpretar("texto", "en", "demo (reglas)");
        assertThat(r.valores().get(0).explicacion()).startsWith("This test measures the amount of sugar")
                .contains("above the laboratory's reference range");
        assertThat(r.resumen()).startsWith("Your report has 12 values; 4 are outside");
        assertThat(r.preguntas()).contains("What does it mean for me that my hemoglobina is below the reference range?",
                "How do these results compare with my previous tests?");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Triglicéridos|alto|es|¿Qué significa para mí que mis triglicéridos estén por encima del rango de referencia?",
            "Vitamina D|bajo|es|¿Qué significa para mí que mi vitamina D esté por debajo del rango de referencia?",
            "Colesterol HDL|bajo|es|¿Qué significa para mí que mi colesterol HDL esté por debajo del rango de referencia?",
            "Total cholesterol|alto|en|What does it mean for me that my total cholesterol is above the reference range?",
            "Triglycerides|alto|en|What does it mean for me that my triglycerides are above the reference range?",
    })
    void preguntasConConcordancia(String nombre, String estado, String idioma, String esperada) {
        assertThat(MotorReglasService.preguntaFueraDeRango(nombre, estado, idioma)).isEqualTo(esperada);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Glucosa en ayunas|es|azúcar en la sangre",
            "Fasting glucose|en|sugar in your blood",
            "Hemoglobina glicosilada|en|two to three months",
            "Hemoglobina|en|carries oxygen",
            "Vitamina D|es|calcio",
            "Platelet count|en|clot",
    })
    void glosarioBilingue(String nombre, String idioma, String fragmento) {
        assertThat(MotorReglasService.queMide(nombre, idioma)).contains(fragmento);
    }

    @Test
    void glosarioSinCoincidencia() {
        assertThat(MotorReglasService.queMide("Ferritina", "en")).isNull();
    }
}
