package co.edu.ucc.informeclaro.infraestructura.service;

import co.edu.ucc.informeclaro.grpc.informes.EstadisticasInformesReply;
import co.edu.ucc.informeclaro.grpc.informes.InformesDeUsuario;
import co.edu.ucc.informeclaro.grpc.interpretacion.EstadisticasInterpretacionReply;
import co.edu.ucc.informeclaro.grpc.interpretacion.UsoMotor;
import co.edu.ucc.informeclaro.grpc.usuarios.UsuarioReply;
import co.edu.ucc.informeclaro.infraestructura.cliente.InformesGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.cliente.InterpretacionGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.cliente.UsuariosGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.dto.EstadisticasAdminOut;
import co.edu.ucc.informeclaro.infraestructura.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** El panel une las cifras que cada microservicio entrega por gRPC; solo un admin puede verlo. */
class AdminServiceTest {

    private final UsuariosGrpcCliente usuarios = mock(UsuariosGrpcCliente.class);
    private final InformesGrpcCliente informes = mock(InformesGrpcCliente.class);
    private final InterpretacionGrpcCliente interpretacion = mock(InterpretacionGrpcCliente.class);

    private static UsuarioReply cuenta(long id, String nombre, String rol) {
        return UsuarioReply.newBuilder().setId(id).setNombre(nombre).setEmail(nombre.toLowerCase() + "@ejemplo.com").setRol(rol).build();
    }

    @Test
    void uneLasCuentasConSusInformesYOrdenaPorActividad() {
        when(usuarios.listar()).thenReturn(List.of(cuenta(1, "Admin", "admin"), cuenta(2, "Ana", "usuario"),
                cuenta(3, "Beto", "")));
        when(informes.estadisticas()).thenReturn(EstadisticasInformesReply.newBuilder()
                .setTotalInformes(3).setTotalValores(12).setFueraDeRango(5)
                .addPorUsuario(InformesDeUsuario.newBuilder().setUsuarioId(3).setInformes(2).setUltimoInforme("2026-10-01T10:00:00Z"))
                .addPorUsuario(InformesDeUsuario.newBuilder().setUsuarioId(2).setInformes(1).setUltimoInforme("2026-09-01T10:00:00Z"))
                .build());
        when(interpretacion.estadisticas()).thenReturn(EstadisticasInterpretacionReply.newBuilder()
                .setTotal(3).setRespaldo(1)
                .addMotores(UsoMotor.newBuilder().setMotor("gemini-flash-lite-latest").setInterpretaciones(2).setDuracionPromedioMs(9000))
                .build());

        EstadisticasAdminOut panel = new AdminService(usuarios, informes, interpretacion).estadisticas();

        assertThat(panel.totales().usuarios()).isEqualTo(3);
        assertThat(panel.totales().administradores()).isEqualTo(1);
        assertThat(panel.totales().fueraDeRango()).isEqualTo(5);
        assertThat(panel.usuarios()).extracting(EstadisticasAdminOut.Usuario::nombre).containsExactly("Beto", "Ana", "Admin");
        assertThat(panel.usuarios().get(0).rol()).isEqualTo("usuario");  // rol vacío = cuenta antigua
        assertThat(panel.usuarios().get(2).ultimoInforme()).isNull();
        assertThat(panel.interpretaciones().respaldo()).isEqualTo(1);
        assertThat(panel.motores()).singleElement().satisfies(m -> assertThat(m.duracionPromedioMs()).isEqualTo(9000));
    }

    @Test
    void unUsuarioSinRolDeAdminRecibe403() {
        when(usuarios.validarToken(anyString())).thenReturn(cuenta(2, "Ana", "usuario"));
        ApiException e = catchThrowableOfType(ApiException.class,
                () -> new AutenticacionService(usuarios).administradorActual("Bearer token"));
        assertThat(e.estado()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(e.clave()).isEqualTo("solo_admin");

        when(usuarios.validarToken(anyString())).thenReturn(cuenta(1, "Admin", "admin"));
        assertThat(new AutenticacionService(usuarios).administradorActual("Bearer token").getId()).isEqualTo(1);
    }
}
