package co.edu.ucc.informeclaro.comun.grpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import java.util.concurrent.TimeUnit;

/** Canales cliente gRPC hacia otros microservicios (HTTP/2 sobre la red interna). */
public final class CanalesGrpc {

    private CanalesGrpc() {
    }

    /**
     * @param destino "host:puerto" del microservicio, p. ej. {@code servicio-usuarios:9091}
     */
    public static ManagedChannel crear(String destino) {
        return NettyChannelBuilder.forTarget(destino)
                .usePlaintext()                      // HTTP/2 en texto plano (h2c) dentro de la red interna
                .maxInboundMessageSize(32 * 1024 * 1024)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .build();
    }
}
