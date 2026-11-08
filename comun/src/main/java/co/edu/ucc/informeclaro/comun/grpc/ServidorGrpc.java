package co.edu.ucc.informeclaro.comun.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Servidor gRPC de un microservicio. Usa Netty, que habla HTTP/2 de forma nativa
 * (h2c en texto plano dentro de la red interna de los servicios).
 *
 * <p>Arranca y se detiene con el contexto de Spring. Mantiene un hilo no daemon para
 * que el proceso siga vivo aunque el servicio no tenga servidor web.</p>
 */
public class ServidorGrpc implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ServidorGrpc.class);

    private final int puerto;
    private final List<BindableService> apis;
    private Server servidor;
    private Thread guardian;

    public ServidorGrpc(int puerto, List<BindableService> apis) {
        this.puerto = puerto;
        this.apis = List.copyOf(apis);
    }

    @Override
    public void start() {
        NettyServerBuilder constructor = NettyServerBuilder.forPort(puerto)
                .maxInboundMessageSize(32 * 1024 * 1024); // un PDF de hasta 10 MB cabe con holgura
        apis.forEach(constructor::addService);
        try {
            servidor = constructor.build().start();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo abrir el puerto gRPC " + puerto, e);
        }
        log.info("Servidor gRPC (HTTP/2) escuchando en el puerto {} con {}", puerto,
                apis.stream().map(a -> a.bindService().getServiceDescriptor().getName()).toList());

        guardian = new Thread(() -> {
            try {
                servidor.awaitTermination();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "grpc-guardian");
        guardian.setDaemon(false);
        guardian.start();
    }

    @Override
    public void stop() {
        if (servidor != null) {
            servidor.shutdown();
            try {
                if (!servidor.awaitTermination(5, TimeUnit.SECONDS)) {
                    servidor.shutdownNow();
                }
            } catch (InterruptedException e) {
                servidor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return servidor != null && !servidor.isShutdown();
    }

    /** Puerto real (útil en pruebas cuando se pide el puerto 0). */
    public int puerto() {
        return servidor != null ? servidor.getPort() : puerto;
    }
}
