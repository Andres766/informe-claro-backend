package co.edu.ucc.informeclaro.documentos.service;

import co.edu.ucc.informeclaro.documentos.dto.ValorExtraidoDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractorValoresServiceTest {

    private final ExtractorValoresService extractor = new ExtractorValoresService(new ClasificadorService());

    @Test
    void reconoceNombresConNumerosYComaDecimal() {
        List<ValorExtraidoDto> valores = extractor.extraer("Vitamina B12 420 pg/mL 200 - 900\nCreatinina 1,3 mg/dL 0,7 - 1,2");
        assertThat(valores).extracting(ValorExtraidoDto::nombreValor, ValorExtraidoDto::valor, ValorExtraidoDto::estado)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Vitamina B12", "420", "normal"),
                        org.assertj.core.groups.Tuple.tuple("Creatinina", "1,3", "alto"));
    }

    @Test
    void ignoraLineasQueNoSonValores() {
        assertThat(extractor.extraer("LABORATORIO CLÍNICO\nPaciente: [OCULTO]\nFecha de toma: 2026-09-20")).isEmpty();
    }
}
