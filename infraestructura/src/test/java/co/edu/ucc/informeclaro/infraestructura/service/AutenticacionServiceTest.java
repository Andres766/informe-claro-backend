package co.edu.ucc.informeclaro.infraestructura.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AutenticacionServiceTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULO", value = {
            "Bearer abc.def|abc.def",
            "bearer   abc|abc",
            "Bearer |NULO",
            "Basic abc|NULO",
            "NULO|NULO",
    })
    void extraeElToken(String cabecera, String esperado) {
        assertThat(AutenticacionService.extraerToken(cabecera)).isEqualTo(esperado);
    }
}
