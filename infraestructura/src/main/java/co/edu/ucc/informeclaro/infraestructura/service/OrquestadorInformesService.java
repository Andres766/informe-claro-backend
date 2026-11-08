package co.edu.ucc.informeclaro.infraestructura.service;

import co.edu.ucc.informeclaro.grpc.informes.GuardarInformeRequest;
import co.edu.ucc.informeclaro.grpc.informes.InformeReply;
import co.edu.ucc.informeclaro.grpc.informes.ValorInforme;
import co.edu.ucc.informeclaro.grpc.interpretacion.InterpretacionReply;
import co.edu.ucc.informeclaro.infraestructura.cliente.DocumentosGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.cliente.InformesGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.cliente.InterpretacionGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.dto.InformeDetalleOut;
import co.edu.ucc.informeclaro.infraestructura.dto.InformeResumenOut;
import co.edu.ucc.informeclaro.infraestructura.dto.PreguntaOut;
import co.edu.ucc.informeclaro.infraestructura.dto.ValorOut;
import co.edu.ucc.informeclaro.infraestructura.error.ApiException;
import co.edu.ucc.informeclaro.infraestructura.i18n.Idiomas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Orquesta el flujo de un informe entre los microservicios, siempre por gRPC:
 * documentos (leer y anonimizar) → interpretación (explicar) → informes (guardar).
 */
@Service
public class OrquestadorInformesService {

    private final DocumentosGrpcCliente documentos;
    private final InterpretacionGrpcCliente interpretacion;
    private final InformesGrpcCliente informes;
    private final int maxPdfMb;

    public OrquestadorInformesService(DocumentosGrpcCliente documentos, InterpretacionGrpcCliente interpretacion,
                                      InformesGrpcCliente informes, @Value("${informeclaro.max-pdf-mb}") int maxPdfMb) {
        this.documentos = documentos;
        this.interpretacion = interpretacion;
        this.informes = informes;
        this.maxPdfMb = maxPdfMb;
    }

    public InformeDetalleOut subir(long usuarioId, String nombreArchivo, String tipoContenido, byte[] contenido,
                                   String idiomaPedido, String idiomaNavegador) {
        if (idiomaPedido != null && !Idiomas.soportado(idiomaPedido)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "idioma_no_soportado");
        }
        String nombre = nombreArchivo == null || nombreArchivo.isBlank() ? "informe.pdf" : nombreArchivo;
        if (!"application/pdf".equals(tipoContenido) && !nombre.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "solo_pdf");
        }
        if (contenido.length > maxPdfMb * 1024L * 1024L) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE, "pdf_grande", maxPdfMb);
        }

        // La explicación se genera en el idioma pedido; si no viene, en el del navegador.
        String idioma = idiomaPedido != null ? Idiomas.normalizar(idiomaPedido, idiomaNavegador) : idiomaNavegador;

        String texto = documentos.procesarPdf(contenido, nombre);       // gRPC → documentos
        InterpretacionReply r = interpretacion.interpretar(texto, idioma); // gRPC → interpretación (→ REST Gemini/Claude)
        if (r.getValoresCount() == 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "sin_valores");
        }
        InformeReply guardado = informes.guardar(GuardarInformeRequest.newBuilder() // gRPC → informes
                .setUsuarioId(usuarioId).setNombreArchivo(nombre).setResumen(r.getResumen())
                .setMotor(r.getMotor()).setIdioma(idioma)
                .addAllValores(r.getValoresList().stream().map(v -> ValorInforme.newBuilder()
                        .setNombreValor(v.getNombreValor()).setUnidad(v.getUnidad()).setValor(v.getValor())
                        .setRangoReferencia(v.getRangoReferencia()).setEstado(v.getEstado())
                        .setExplicacionIa(v.getExplicacion()).build()).toList())
                .addAllPreguntas(r.getPreguntasList())
                .build());
        return detalle(guardado);
    }

    public List<InformeResumenOut> listar(long usuarioId) {
        return informes.listar(usuarioId).stream()
                .map(i -> new InformeResumenOut(i.getId(), i.getNombreArchivo(), i.getFechaCarga(), i.getMotor(),
                        i.getIdioma(), i.getTotalValores(), i.getFueraDeRango()))
                .toList();
    }

    public InformeDetalleOut obtener(long id, long usuarioId) {
        return detalle(informes.obtener(id, usuarioId));
    }

    public void eliminar(long id, long usuarioId) {
        informes.eliminar(id, usuarioId);
    }

    private InformeDetalleOut detalle(InformeReply i) {
        return new InformeDetalleOut(i.getId(), i.getNombreArchivo(), i.getFechaCarga(), i.getMotor(), i.getIdioma(),
                i.getResumen(),
                i.getValoresList().stream().map(v -> new ValorOut(v.getId(), v.getNombreValor(), v.getUnidad(),
                        v.getValor(), v.getRangoReferencia(), v.getEstado(), v.getExplicacionIa())).toList(),
                i.getPreguntasList().stream().map(p -> new PreguntaOut(p.getId(), p.getTextoPregunta())).toList(),
                interpretacion.aviso(i.getIdioma())); // el aviso médico lo define el microservicio de interpretación
    }
}
