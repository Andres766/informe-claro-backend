package co.edu.ucc.informeclaro.usuarios.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordServiceTest {

    @Test
    void recortaA72BytesSinPartirCaracteres() {
        String larga = "ñ".repeat(70); // 140 bytes
        String recortada = PasswordService.recortar(larga);
        assertThat(recortada.getBytes(StandardCharsets.UTF_8).length).isEqualTo(72);
        assertThat(recortada).isEqualTo("ñ".repeat(36));
    }

    @Test
    void hasheaYVerifica() {
        PasswordService servicio = new PasswordService();
        String hash = servicio.hashear("clave-segura-1");
        assertThat(servicio.coincide("clave-segura-1", hash)).isTrue();
        assertThat(servicio.coincide("otra", hash)).isFalse();
        assertThat(servicio.coincide("clave-segura-1", null)).isFalse();
    }
}
