package co.edu.ucc.informeclaro.infraestructura.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class IdiomasTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULO", value = {
            "NULO|es",
            "en-US,en;q=0.9|en",
            "es-CO,es;q=0.9,en;q=0.8|es",
            "fr-FR,en;q=0.5|en",
            "de-DE|es",
            "es;q=0.4,en;q=0.9|en",
    })
    void normaliza(String cabecera, String esperado) {
        assertThat(Idiomas.normalizar(cabecera, "es")).isEqualTo(esperado);
    }

    @Test
    void mensajesTraducidos() {
        assertThat(Mensajes.t("pdf_grande", "en", 10)).isEqualTo("The PDF is larger than 10 MB");
        assertThat(Mensajes.t("pdf_grande", "es", 10)).isEqualTo("El PDF supera 10 MB");
        assertThat(Mensajes.t("credenciales", "fr")).isEqualTo("Correo o contraseña incorrectos");
    }

    @Test
    void validaIdiomaPedido() {
        assertThat(Idiomas.soportado("en")).isTrue();
        assertThat(Idiomas.soportado("es-CO")).isTrue();
        assertThat(Idiomas.soportado("fr")).isFalse();
    }
}
