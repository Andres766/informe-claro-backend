package co.edu.ucc.informeclaro.informes;

import co.edu.ucc.informeclaro.comun.grpc.CanalesGrpc;
import co.edu.ucc.informeclaro.comun.grpc.ServidorGrpc;
import co.edu.ucc.informeclaro.grpc.informes.GuardarInformeRequest;
import co.edu.ucc.informeclaro.grpc.informes.InformeIdRequest;
import co.edu.ucc.informeclaro.grpc.informes.InformeReply;
import co.edu.ucc.informeclaro.grpc.informes.InformeResumen;
import co.edu.ucc.informeclaro.grpc.informes.InformesServicioGrpc;
import co.edu.ucc.informeclaro.grpc.informes.ListarRequest;
import co.edu.ucc.informeclaro.grpc.informes.ValorInforme;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@SpringBootTest
@ActiveProfiles("test")
class InformesGrpcApiTest {

    @Autowired
    ServidorGrpc servidor;

    ManagedChannel canal;

    InformesServicioGrpc.InformesServicioBlockingStub cliente() {
        if (canal == null) {
            canal = CanalesGrpc.crear("127.0.0.1:" + servidor.puerto());
        }
        return InformesServicioGrpc.newBlockingStub(canal);
    }

    @AfterEach
    void cerrar() {
        if (canal != null) {
            canal.shutdownNow();
        }
    }

    private InformeReply guardar(long usuario, String archivo) {
        return cliente().guardar(GuardarInformeRequest.newBuilder()
                .setUsuarioId(usuario).setNombreArchivo(archivo).setResumen("Resumen").setMotor("demo (reglas)").setIdioma("en")
                .addValores(ValorInforme.newBuilder().setNombreValor("Glucosa").setUnidad("mg/dL").setValor("112")
                        .setRangoReferencia("70 - 100").setEstado("alto").setExplicacionIa("Explicación"))
                .addValores(ValorInforme.newBuilder().setNombreValor("TSH").setUnidad("uUI/mL").setValor("2.4")
                        .setRangoReferencia("0.4 - 4.0").setEstado("normal").setExplicacionIa("Explicación"))
                .addAllPreguntas(List.of("¿Primera?", "¿Segunda?"))
                .build());
    }

    @Test
    void guardaListaObtieneYEliminaConAislamientoPorUsuario() {
        InformeReply informe = guardar(101, "mio.pdf");
        assertThat(informe.getValoresCount()).isEqualTo(2);
        assertThat(informe.getPreguntasList()).extracting(p -> p.getTextoPregunta()).containsExactly("¿Primera?", "¿Segunda?");
        assertThat(Instant.parse(informe.getFechaCarga())).isBeforeOrEqualTo(Instant.now());

        List<InformeResumen> historial = cliente().listar(ListarRequest.newBuilder().setUsuarioId(101).build()).getInformesList();
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).getFueraDeRango()).isEqualTo(1);
        assertThat(historial.get(0).getTotalValores()).isEqualTo(2);
        assertThat(historial.get(0).getIdioma()).isEqualTo("en");

        // Otro usuario no lo ve ni lo puede borrar.
        assertThat(cliente().listar(ListarRequest.newBuilder().setUsuarioId(202).build()).getInformesList()).isEmpty();
        StatusRuntimeException ajeno = catchThrowableOfType(StatusRuntimeException.class, () -> cliente().eliminar(
                InformeIdRequest.newBuilder().setId(informe.getId()).setUsuarioId(202).build()));
        assertThat(ajeno.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(ajeno.getStatus().getDescription()).isEqualTo("informe_no_encontrado");

        cliente().eliminar(InformeIdRequest.newBuilder().setId(informe.getId()).setUsuarioId(101).build());
        StatusRuntimeException borrado = catchThrowableOfType(StatusRuntimeException.class, () -> cliente().obtener(
                InformeIdRequest.newBuilder().setId(informe.getId()).setUsuarioId(101).build()));
        assertThat(borrado.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void elHistorialVaDelMasRecienteAlMasAntiguo() {
        guardar(303, "primero.pdf");
        guardar(303, "segundo.pdf");
        assertThat(cliente().listar(ListarRequest.newBuilder().setUsuarioId(303).build()).getInformesList())
                .extracting(InformeResumen::getNombreArchivo).containsExactly("segundo.pdf", "primero.pdf");
    }
}
