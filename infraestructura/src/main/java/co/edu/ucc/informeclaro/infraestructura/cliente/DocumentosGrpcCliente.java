package co.edu.ucc.informeclaro.infraestructura.cliente;

import co.edu.ucc.informeclaro.grpc.documentos.DocumentosServicioGrpc;
import co.edu.ucc.informeclaro.grpc.documentos.PdfRequest;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Cliente gRPC (HTTP/2) del microservicio de documentos. */
@Component
public class DocumentosGrpcCliente {

    private final DocumentosServicioGrpc.DocumentosServicioBlockingStub stub;

    public DocumentosGrpcCliente(DocumentosServicioGrpc.DocumentosServicioBlockingStub stub) {
        this.stub = stub;
    }

    /** Envía el PDF (en memoria) y recibe el texto ya anonimizado. */
    public String procesarPdf(byte[] contenido, String nombreArchivo) {
        return stub.withDeadlineAfter(60, TimeUnit.SECONDS)
                .procesarPdf(PdfRequest.newBuilder().setContenido(ByteString.copyFrom(contenido))
                        .setNombreArchivo(nombreArchivo).build())
                .getTextoAnonimizado();
    }
}
