package co.edu.ucc.informeclaro.infraestructura.config;

import co.edu.ucc.informeclaro.comun.grpc.CanalesGrpc;
import co.edu.ucc.informeclaro.grpc.documentos.DocumentosServicioGrpc;
import co.edu.ucc.informeclaro.grpc.informes.InformesServicioGrpc;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretacionServicioGrpc;
import co.edu.ucc.informeclaro.grpc.usuarios.UsuariosServicioGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Canales gRPC (HTTP/2) del orquestador hacia cada microservicio. */
@Configuration(proxyBeanMethods = false)
public class ClientesGrpcConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel canalUsuarios(@Value("${informeclaro.servicios.usuarios}") String destino) {
        return CanalesGrpc.crear(destino);
    }

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel canalDocumentos(@Value("${informeclaro.servicios.documentos}") String destino) {
        return CanalesGrpc.crear(destino);
    }

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel canalInterpretacion(@Value("${informeclaro.servicios.interpretacion}") String destino) {
        return CanalesGrpc.crear(destino);
    }

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel canalInformes(@Value("${informeclaro.servicios.informes}") String destino) {
        return CanalesGrpc.crear(destino);
    }

    @Bean
    public UsuariosServicioGrpc.UsuariosServicioBlockingStub stubUsuarios(@Qualifier("canalUsuarios") ManagedChannel canal) {
        return UsuariosServicioGrpc.newBlockingStub(canal);
    }

    @Bean
    public DocumentosServicioGrpc.DocumentosServicioBlockingStub stubDocumentos(@Qualifier("canalDocumentos") ManagedChannel canal) {
        return DocumentosServicioGrpc.newBlockingStub(canal);
    }

    @Bean
    public InterpretacionServicioGrpc.InterpretacionServicioBlockingStub stubInterpretacion(@Qualifier("canalInterpretacion") ManagedChannel canal) {
        return InterpretacionServicioGrpc.newBlockingStub(canal);
    }

    @Bean
    public InformesServicioGrpc.InformesServicioBlockingStub stubInformes(@Qualifier("canalInformes") ManagedChannel canal) {
        return InformesServicioGrpc.newBlockingStub(canal);
    }
}
