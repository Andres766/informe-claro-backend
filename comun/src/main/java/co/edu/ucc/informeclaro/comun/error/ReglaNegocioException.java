package co.edu.ucc.informeclaro.comun.error;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Error de negocio de un microservicio. Viaja por gRPC como un {@link Status} cuyo código indica
 * el tipo de error y cuya descripción es la clave del mensaje traducible (la traduce el orquestador).
 */
public class ReglaNegocioException extends RuntimeException {

    private final Status.Code codigo;
    private final String clave;

    public ReglaNegocioException(Status.Code codigo, String clave) {
        super(clave);
        this.codigo = codigo;
        this.clave = clave;
    }

    public Status.Code codigo() {
        return codigo;
    }

    public String clave() {
        return clave;
    }

    public StatusRuntimeException aGrpc() {
        return Status.fromCode(codigo).withDescription(clave).asRuntimeException();
    }
}
