package co.edu.ucc.informeclaro.interpretacion;

import co.edu.ucc.informeclaro.comun.grpc.CanalesGrpc;
import co.edu.ucc.informeclaro.comun.grpc.ServidorGrpc;
import co.edu.ucc.informeclaro.grpc.comun.Vacio;
import co.edu.ucc.informeclaro.grpc.interpretacion.AvisoRequest;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretacionReply;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretacionServicioGrpc;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretarRequest;
import co.edu.ucc.informeclaro.interpretacion.cliente.ClienteDocumentos;
import co.edu.ucc.informeclaro.interpretacion.repository.RegistroInterpretacionRepository;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** El microservicio completo atendido por su API gRPC, en modo demo (sin API key). */
@SpringBootTest
@ActiveProfiles("test")
class InterpretacionGrpcApiTest {

    @TestConfiguration
    static class Falsos {
        @Bean
        @Primary
        ClienteDocumentos documentosFalso() {
            return new DocumentosFalso();
        }
    }

    @Autowired
    ServidorGrpc servidor;
    @Autowired
    RegistroInterpretacionRepository registros;

    ManagedChannel canal;

    InterpretacionServicioGrpc.InterpretacionServicioBlockingStub cliente() {
        if (canal == null) {
            canal = CanalesGrpc.crear("127.0.0.1:" + servidor.puerto());
        }
        return InterpretacionServicioGrpc.newBlockingStub(canal);
    }

    @AfterEach
    void cerrar() {
        if (canal != null) {
            canal.shutdownNow();
        }
    }

    @Test
    void interpretaEnModoDemoYLoRegistra() {
        InterpretacionReply r = cliente().interpretar(InterpretarRequest.newBuilder().setTexto("texto").setIdioma("en").build());
        assertThat(r.getMotor()).isEqualTo("demo (reglas)");
        assertThat(r.getValoresCount()).isEqualTo(12);
        assertThat(r.getResumen()).startsWith("Your report has 12 values");
        var registro = registros.findTopByOrderByIdDesc().orElseThrow();
        assertThat(registro.getIdioma()).isEqualTo("en");
        assertThat(registro.getTotalValores()).isEqualTo(12);
    }

    @Test
    void avisoYEstado() {
        assertThat(cliente().obtenerAviso(AvisoRequest.newBuilder().setIdioma("en").build()).getAviso())
                .startsWith("This explanation is for information only");
        assertThat(cliente().obtenerAviso(AvisoRequest.newBuilder().setIdioma("es").build()).getAviso())
                .contains("médico tratante");
        assertThat(cliente().estado(Vacio.getDefaultInstance()).getMotor()).isEqualTo("demo (reglas)");
    }
}
