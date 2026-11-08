package co.edu.ucc.informeclaro.comun.datos;

import co.edu.ucc.informeclaro.comun.datos.ConfiguracionBaseDatos.ConexionJdbc;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConexionJdbcTest {

    @Test
    void convierteLaUrlQueEntregaRender() {
        ConexionJdbc c = ConexionJdbc.desde("postgresql://admin:cl%40ve@dpg-x.oregon-postgres.render.com/informeclaro", "", "");
        assertThat(c.jdbcUrl()).isEqualTo("jdbc:postgresql://dpg-x.oregon-postgres.render.com:5432/informeclaro");
        assertThat(c.usuario()).isEqualTo("admin");
        assertThat(c.clave()).isEqualTo("cl@ve");
    }

    @Test
    void aceptaElPrefijoPostgresYConservaParametros() {
        ConexionJdbc c = ConexionJdbc.desde("postgres://u:p@host:6543/base?sslmode=require", "", "");
        assertThat(c.jdbcUrl()).isEqualTo("jdbc:postgresql://host:6543/base?sslmode=require");
    }

    @Test
    void usaUnaUrlJdbcTalCual() {
        ConexionJdbc c = ConexionJdbc.desde("jdbc:h2:mem:prueba", "sa", "");
        assertThat(c.jdbcUrl()).isEqualTo("jdbc:h2:mem:prueba");
        assertThat(c.usuario()).isEqualTo("sa");
    }

    @Test
    void rechazaOtrosMotores() {
        assertThatThrownBy(() -> ConexionJdbc.desde("mysql://x/y", "", "")).isInstanceOf(IllegalArgumentException.class);
    }
}
