package co.edu.ucc.informeclaro.informes.api;

import co.edu.ucc.informeclaro.comun.grpc.RespuestaGrpc;
import co.edu.ucc.informeclaro.grpc.comun.Vacio;
import co.edu.ucc.informeclaro.grpc.informes.GuardarInformeRequest;
import co.edu.ucc.informeclaro.grpc.informes.InformeIdRequest;
import co.edu.ucc.informeclaro.grpc.informes.InformeReply;
import co.edu.ucc.informeclaro.grpc.informes.InformeResumen;
import co.edu.ucc.informeclaro.grpc.informes.InformesServicioGrpc;
import co.edu.ucc.informeclaro.grpc.informes.ListaInformesReply;
import co.edu.ucc.informeclaro.grpc.informes.ListarRequest;
import co.edu.ucc.informeclaro.grpc.informes.PreguntaInforme;
import co.edu.ucc.informeclaro.grpc.informes.ValorInforme;
import co.edu.ucc.informeclaro.informes.controller.InformeController;
import co.edu.ucc.informeclaro.informes.dto.InformeDto;
import co.edu.ucc.informeclaro.informes.dto.NuevoInformeDto;
import co.edu.ucc.informeclaro.informes.dto.ValorDto;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

/** API gRPC (HTTP/2) del microservicio de informes: traduce protobuf ⇄ DTO y delega en el controlador. */
@Component
public class InformesGrpcApi extends InformesServicioGrpc.InformesServicioImplBase {

    private final InformeController controlador;

    public InformesGrpcApi(InformeController controlador) {
        this.controlador = controlador;
    }

    @Override
    public void guardar(GuardarInformeRequest p, StreamObserver<InformeReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> aReply(controlador.guardar(new NuevoInformeDto(
                p.getUsuarioId(), p.getNombreArchivo(), p.getResumen(), p.getMotor(), p.getIdioma(),
                p.getValoresList().stream().map(v -> new ValorDto(null, v.getNombreValor(), v.getUnidad(), v.getValor(),
                        v.getRangoReferencia(), v.getEstado(), v.getExplicacionIa())).toList(),
                p.getPreguntasList()))));
    }

    @Override
    public void listar(ListarRequest p, StreamObserver<ListaInformesReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> ListaInformesReply.newBuilder()
                .addAllInformes(controlador.listar(p.getUsuarioId()).stream().map(i -> InformeResumen.newBuilder()
                        .setId(i.id()).setNombreArchivo(i.nombreArchivo()).setFechaCarga(i.fechaCarga().toString())
                        .setMotor(i.motor()).setIdioma(i.idioma()).setTotalValores(i.totalValores())
                        .setFueraDeRango(i.fueraDeRango()).build()).toList())
                .build());
    }

    @Override
    public void obtener(InformeIdRequest p, StreamObserver<InformeReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> aReply(controlador.obtener(p.getId(), p.getUsuarioId())));
    }

    @Override
    public void eliminar(InformeIdRequest p, StreamObserver<Vacio> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> {
            controlador.eliminar(p.getId(), p.getUsuarioId());
            return Vacio.getDefaultInstance();
        });
    }

    private static InformeReply aReply(InformeDto i) {
        return InformeReply.newBuilder()
                .setId(i.id()).setNombreArchivo(i.nombreArchivo()).setFechaCarga(i.fechaCarga().toString())
                .setMotor(i.motor()).setIdioma(i.idioma()).setResumen(i.resumen())
                .addAllValores(i.valores().stream().map(v -> ValorInforme.newBuilder()
                        .setId(v.id()).setNombreValor(v.nombreValor()).setUnidad(v.unidad()).setValor(v.valor())
                        .setRangoReferencia(v.rangoReferencia()).setEstado(v.estado()).setExplicacionIa(v.explicacionIa())
                        .build()).toList())
                .addAllPreguntas(i.preguntas().stream().map(q -> PreguntaInforme.newBuilder()
                        .setId(q.id()).setTextoPregunta(q.textoPregunta()).build()).toList())
                .build();
    }
}
