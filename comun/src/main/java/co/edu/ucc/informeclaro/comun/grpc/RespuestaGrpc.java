package co.edu.ucc.informeclaro.comun.grpc;

import co.edu.ucc.informeclaro.comun.error.ReglaNegocioException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Ejecuta una operación unaria y entrega su resultado (o su error) al cliente gRPC.
 * Los errores de negocio conservan su código y clave; cualquier otro error sale como INTERNAL.
 */
public final class RespuestaGrpc {

    private static final Logger log = LoggerFactory.getLogger(RespuestaGrpc.class);

    private RespuestaGrpc() {
    }

    public static <T> void responder(StreamObserver<T> observador, Supplier<T> operacion) {
        try {
            observador.onNext(operacion.get());
            observador.onCompleted();
        } catch (ReglaNegocioException e) {
            observador.onError(e.aGrpc());
        } catch (RuntimeException e) {
            log.error("Error inesperado atendiendo una llamada gRPC", e);
            observador.onError(Status.INTERNAL.withDescription("error_interno").withCause(e).asRuntimeException());
        }
    }
}
