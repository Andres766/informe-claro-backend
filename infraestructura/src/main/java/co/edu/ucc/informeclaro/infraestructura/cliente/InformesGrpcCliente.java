package co.edu.ucc.informeclaro.infraestructura.cliente;

import co.edu.ucc.informeclaro.grpc.informes.GuardarInformeRequest;
import co.edu.ucc.informeclaro.grpc.informes.InformeIdRequest;
import co.edu.ucc.informeclaro.grpc.informes.InformeReply;
import co.edu.ucc.informeclaro.grpc.informes.InformeResumen;
import co.edu.ucc.informeclaro.grpc.informes.InformesServicioGrpc;
import co.edu.ucc.informeclaro.grpc.informes.ListarRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Cliente gRPC (HTTP/2) del microservicio de informes. */
@Component
public class InformesGrpcCliente {

    private final InformesServicioGrpc.InformesServicioBlockingStub stub;

    public InformesGrpcCliente(InformesServicioGrpc.InformesServicioBlockingStub stub) {
        this.stub = stub;
    }

    private InformesServicioGrpc.InformesServicioBlockingStub conPlazo() {
        return stub.withDeadlineAfter(20, TimeUnit.SECONDS);
    }

    public InformeReply guardar(GuardarInformeRequest peticion) {
        return conPlazo().guardar(peticion);
    }

    public List<InformeResumen> listar(long usuarioId) {
        return conPlazo().listar(ListarRequest.newBuilder().setUsuarioId(usuarioId).build()).getInformesList();
    }

    public InformeReply obtener(long id, long usuarioId) {
        return conPlazo().obtener(InformeIdRequest.newBuilder().setId(id).setUsuarioId(usuarioId).build());
    }

    public void eliminar(long id, long usuarioId) {
        conPlazo().eliminar(InformeIdRequest.newBuilder().setId(id).setUsuarioId(usuarioId).build());
    }
}
