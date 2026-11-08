package co.edu.ucc.informeclaro.interpretacion.cliente;

import co.edu.ucc.informeclaro.grpc.documentos.ClasificarRequest;
import co.edu.ucc.informeclaro.grpc.documentos.DocumentosServicioGrpc;
import co.edu.ucc.informeclaro.grpc.documentos.TextoRequest;
import co.edu.ucc.informeclaro.grpc.documentos.ValorParaClasificar;
import co.edu.ucc.informeclaro.interpretacion.dto.RangoDto;
import co.edu.ucc.informeclaro.interpretacion.dto.ValorBaseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Cliente gRPC (HTTP/2) del microservicio de documentos. */
@Component
public class DocumentosGrpcCliente implements ClienteDocumentos {

    private final DocumentosServicioGrpc.DocumentosServicioBlockingStub stub;

    public DocumentosGrpcCliente(DocumentosServicioGrpc.DocumentosServicioBlockingStub stub) {
        this.stub = stub;
    }

    @Override
    public List<ValorBaseDto> extraerValores(String texto) {
        return stub.withDeadlineAfter(30, TimeUnit.SECONDS)
                .extraerValores(TextoRequest.newBuilder().setTexto(texto).build())
                .getValoresList().stream()
                .map(v -> new ValorBaseDto(v.getNombreValor(), v.getValor(), v.getUnidad(), v.getRangoReferencia(), v.getEstado()))
                .toList();
    }

    @Override
    public List<String> clasificar(List<RangoDto> valores) {
        ClasificarRequest.Builder peticion = ClasificarRequest.newBuilder();
        valores.forEach(v -> peticion.addValores(ValorParaClasificar.newBuilder()
                .setValor(v.valor()).setRangoReferencia(v.rangoReferencia() == null ? "" : v.rangoReferencia())));
        return stub.withDeadlineAfter(30, TimeUnit.SECONDS).clasificarValores(peticion.build()).getEstadosList();
    }
}
