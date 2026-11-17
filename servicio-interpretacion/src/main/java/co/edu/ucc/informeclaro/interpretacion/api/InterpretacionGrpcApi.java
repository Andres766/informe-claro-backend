package co.edu.ucc.informeclaro.interpretacion.api;

import co.edu.ucc.informeclaro.comun.grpc.RespuestaGrpc;
import co.edu.ucc.informeclaro.grpc.comun.Vacio;
import co.edu.ucc.informeclaro.grpc.interpretacion.AvisoReply;
import co.edu.ucc.informeclaro.grpc.interpretacion.AvisoRequest;
import co.edu.ucc.informeclaro.grpc.interpretacion.EstadisticasInterpretacionReply;
import co.edu.ucc.informeclaro.grpc.interpretacion.EstadoReply;
import co.edu.ucc.informeclaro.grpc.interpretacion.FalloIa;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretacionReply;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretacionServicioGrpc;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretarRequest;
import co.edu.ucc.informeclaro.grpc.interpretacion.UsoMotor;
import co.edu.ucc.informeclaro.grpc.interpretacion.ValorInterpretado;
import co.edu.ucc.informeclaro.interpretacion.controller.InterpretacionController;
import co.edu.ucc.informeclaro.interpretacion.dto.EstadisticasInterpretacionDto;
import co.edu.ucc.informeclaro.interpretacion.dto.InterpretacionDto;
import co.edu.ucc.informeclaro.interpretacion.dto.SolicitudInterpretacionDto;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

/** API gRPC (HTTP/2) del microservicio de interpretación: traduce protobuf ⇄ DTO y delega en el controlador. */
@Component
public class InterpretacionGrpcApi extends InterpretacionServicioGrpc.InterpretacionServicioImplBase {

    private final InterpretacionController controlador;

    public InterpretacionGrpcApi(InterpretacionController controlador) {
        this.controlador = controlador;
    }

    @Override
    public void interpretar(InterpretarRequest peticion, StreamObserver<InterpretacionReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> aReply(controlador.interpretar(
                new SolicitudInterpretacionDto(peticion.getTexto(), peticion.getIdioma()))));
    }

    @Override
    public void obtenerAviso(AvisoRequest peticion, StreamObserver<AvisoReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> AvisoReply.newBuilder().setAviso(controlador.aviso(peticion.getIdioma())).build());
    }

    @Override
    public void estado(Vacio peticion, StreamObserver<EstadoReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> EstadoReply.newBuilder().setMotor(controlador.motor()).build());
    }

    @Override
    public void estadisticas(Vacio peticion, StreamObserver<EstadisticasInterpretacionReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> aReply(controlador.estadisticas()));
    }

    private static EstadisticasInterpretacionReply aReply(EstadisticasInterpretacionDto e) {
        return EstadisticasInterpretacionReply.newBuilder()
                .setTotal((int) e.total()).setDuracionPromedioMs(e.duracionPromedioMs())
                .setGuardrailsActivados((int) e.guardrailsActivados()).setRespaldo((int) e.respaldo())
                .addAllMotores(e.motores().stream().map(m -> UsoMotor.newBuilder().setMotor(m.motor())
                        .setInterpretaciones((int) m.interpretaciones()).setDuracionPromedioMs(m.duracionPromedioMs())
                        .build()).toList())
                .addAllFallosRecientes(e.fallosRecientes().stream().map(f -> FalloIa.newBuilder()
                        .setFecha(f.fecha().toString()).setCausa(f.causa()).build()).toList())
                .build();
    }

    private static InterpretacionReply aReply(InterpretacionDto dto) {
        return InterpretacionReply.newBuilder()
                .setResumen(dto.resumen())
                .setMotor(dto.motor())
                .addAllPreguntas(dto.preguntas())
                .addAllValores(dto.valores().stream().map(v -> ValorInterpretado.newBuilder()
                        .setNombreValor(v.nombreValor()).setValor(v.valor()).setUnidad(v.unidad())
                        .setRangoReferencia(v.rangoReferencia()).setEstado(v.estado()).setExplicacion(v.explicacion())
                        .build()).toList())
                .build();
    }
}
