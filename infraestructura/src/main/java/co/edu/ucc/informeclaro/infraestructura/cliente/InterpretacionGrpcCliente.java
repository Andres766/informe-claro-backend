package co.edu.ucc.informeclaro.infraestructura.cliente;

import co.edu.ucc.informeclaro.grpc.comun.Vacio;
import co.edu.ucc.informeclaro.grpc.interpretacion.AvisoRequest;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretacionReply;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretacionServicioGrpc;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretarRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Cliente gRPC (HTTP/2) del microservicio de interpretación. */
@Component
public class InterpretacionGrpcCliente {

    private final InterpretacionServicioGrpc.InterpretacionServicioBlockingStub stub;

    public InterpretacionGrpcCliente(InterpretacionServicioGrpc.InterpretacionServicioBlockingStub stub) {
        this.stub = stub;
    }

    public InterpretacionReply interpretar(String texto, String idioma) {
        // La IA puede tardar con informes largos; el motor por reglas responde al instante.
        return stub.withDeadlineAfter(6, TimeUnit.MINUTES)
                .interpretar(InterpretarRequest.newBuilder().setTexto(texto).setIdioma(idioma).build());
    }

    public String aviso(String idioma) {
        return stub.withDeadlineAfter(10, TimeUnit.SECONDS)
                .obtenerAviso(AvisoRequest.newBuilder().setIdioma(idioma).build()).getAviso();
    }

    public String motor() {
        return stub.withDeadlineAfter(5, TimeUnit.SECONDS).estado(Vacio.getDefaultInstance()).getMotor();
    }
}
