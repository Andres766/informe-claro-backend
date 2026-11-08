package co.edu.ucc.informeclaro.documentos.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClasificadorServiceTest {

    private final ClasificadorService clasificador = new ClasificadorService();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "112|70 - 100|alto",
            "65|70 - 100|bajo",
            "85|70 - 100|normal",
            "215|< 200|alto",
            "48|> 40|normal",
            "0,9|0,6 - 1,2|normal",
            "positivo|negativo|sin_rango",
            "200|<= 200|normal",
            "40|>= 40|normal",
            "90|texto raro|sin_rango",
    })
    void clasifica(String valor, String rango, String esperado) {
        assertThat(clasificador.clasificar(valor, rango)).isEqualTo(esperado);
    }
}
