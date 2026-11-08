package co.edu.ucc.informeclaro.documentos.api;

import co.edu.ucc.informeclaro.comun.grpc.RespuestaGrpc;
import co.edu.ucc.informeclaro.documentos.controller.DocumentoController;
import co.edu.ucc.informeclaro.documentos.dto.DocumentoDto;
import co.edu.ucc.informeclaro.documentos.dto.ValorParaClasificarDto;
import co.edu.ucc.informeclaro.grpc.documentos.ClasificarReply;
import co.edu.ucc.informeclaro.grpc.documentos.ClasificarRequest;
import co.edu.ucc.informeclaro.grpc.documentos.DocumentoReply;
import co.edu.ucc.informeclaro.grpc.documentos.DocumentosServicioGrpc;
import co.edu.ucc.informeclaro.grpc.documentos.PdfRequest;
import co.edu.ucc.informeclaro.grpc.documentos.TextoRequest;
import co.edu.ucc.informeclaro.grpc.documentos.ValorExtraido;
import co.edu.ucc.informeclaro.grpc.documentos.ValoresReply;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

/** API gRPC (HTTP/2) del microservicio de documentos: traduce protobuf ⇄ DTO y delega en el controlador. */
@Component
public class DocumentosGrpcApi extends DocumentosServicioGrpc.DocumentosServicioImplBase {

    private final DocumentoController controlador;

    public DocumentosGrpcApi(DocumentoController controlador) {
        this.controlador = controlador;
    }

    @Override
    public void procesarPdf(PdfRequest peticion, StreamObserver<DocumentoReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> {
            DocumentoDto doc = controlador.procesarPdf(peticion.getContenido().toByteArray());
            return DocumentoReply.newBuilder().setTextoAnonimizado(doc.textoAnonimizado()).setPaginas(doc.paginas()).build();
        });
    }

    @Override
    public void extraerValores(TextoRequest peticion, StreamObserver<ValoresReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> ValoresReply.newBuilder()
                .addAllValores(controlador.extraerValores(peticion.getTexto()).stream()
                        .map(v -> ValorExtraido.newBuilder()
                                .setNombreValor(v.nombreValor()).setValor(v.valor()).setUnidad(v.unidad())
                                .setRangoReferencia(v.rangoReferencia()).setEstado(v.estado()).build())
                        .toList())
                .build());
    }

    @Override
    public void clasificarValores(ClasificarRequest peticion, StreamObserver<ClasificarReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> ClasificarReply.newBuilder()
                .addAllEstados(controlador.clasificar(peticion.getValoresList().stream()
                        .map(v -> new ValorParaClasificarDto(v.getValor(), v.getRangoReferencia())).toList()))
                .build());
    }
}
