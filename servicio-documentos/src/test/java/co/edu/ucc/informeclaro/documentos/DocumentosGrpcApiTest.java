package co.edu.ucc.informeclaro.documentos;

import co.edu.ucc.informeclaro.comun.grpc.CanalesGrpc;
import co.edu.ucc.informeclaro.comun.grpc.ServidorGrpc;
import co.edu.ucc.informeclaro.documentos.repository.RegistroProcesamientoRepository;
import co.edu.ucc.informeclaro.grpc.documentos.ClasificarRequest;
import co.edu.ucc.informeclaro.grpc.documentos.DocumentoReply;
import co.edu.ucc.informeclaro.grpc.documentos.DocumentosServicioGrpc;
import co.edu.ucc.informeclaro.grpc.documentos.PdfRequest;
import co.edu.ucc.informeclaro.grpc.documentos.TextoRequest;
import co.edu.ucc.informeclaro.grpc.documentos.ValorExtraido;
import co.edu.ucc.informeclaro.grpc.documentos.ValorParaClasificar;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** Procesa los PDF de ejemplo reales a través de la API gRPC del microservicio. */
@SpringBootTest
@ActiveProfiles("test")
class DocumentosGrpcApiTest {

    static final Path MUESTRAS = Path.of("..", "samples");

    @Autowired
    ServidorGrpc servidor;
    @Autowired
    RegistroProcesamientoRepository registros;

    ManagedChannel canal;

    DocumentosServicioGrpc.DocumentosServicioBlockingStub cliente() {
        if (canal == null) {
            canal = CanalesGrpc.crear("127.0.0.1:" + servidor.puerto());
        }
        return DocumentosServicioGrpc.newBlockingStub(canal);
    }

    @AfterEach
    void cerrar() {
        if (canal != null) {
            canal.shutdownNow();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "informe_ejemplo.pdf, 12, Glucosa en ayunas, alto, María Fernanda",
            "informe_ejemplo_2.pdf, 12, Vitamina B12, normal, Carlos Andrés",
            "lab_report_example.pdf, 10, Total cholesterol, alto, Jane Doe",
    })
    void procesaLosInformesDeEjemplo(String archivo, int cantidad, String nombre, String estado, String paciente) throws Exception {
        DocumentoReply doc = cliente().procesarPdf(PdfRequest.newBuilder()
                .setContenido(ByteString.copyFrom(Files.readAllBytes(MUESTRAS.resolve(archivo)))).build());
        assertThat(doc.getTextoAnonimizado()).doesNotContain(paciente).contains("[OCULTO]");
        assertThat(doc.getPaginas()).isEqualTo(1);

        List<ValorExtraido> valores = cliente().extraerValores(
                TextoRequest.newBuilder().setTexto(doc.getTextoAnonimizado()).build()).getValoresList();
        Map<String, String> estados = valores.stream()
                .collect(Collectors.toMap(ValorExtraido::getNombreValor, ValorExtraido::getEstado));
        assertThat(valores).hasSize(cantidad);
        assertThat(estados).containsEntry(nombre, estado);
    }

    @Test
    void elPrimerInformeTieneCuatroValoresFueraDeRango() throws Exception {
        DocumentoReply doc = cliente().procesarPdf(PdfRequest.newBuilder()
                .setContenido(ByteString.copyFrom(Files.readAllBytes(MUESTRAS.resolve("informe_ejemplo.pdf")))).build());
        List<ValorExtraido> valores = cliente().extraerValores(
                TextoRequest.newBuilder().setTexto(doc.getTextoAnonimizado()).build()).getValoresList();
        assertThat(valores.stream().filter(v -> v.getEstado().equals("alto") || v.getEstado().equals("bajo")))
                .extracting(ValorExtraido::getNombreValor)
                .containsExactly("Glucosa en ayunas", "Colesterol total", "Colesterol LDL", "Hemoglobina");
    }

    @Test
    void unArchivoQueNoEsPdfEsIlegibleYQuedaEnLaBitacoraSinContenido() {
        long antes = registros.countByResultado("pdf_ilegible");
        StatusRuntimeException e = catchThrowableOfType(StatusRuntimeException.class, () -> cliente().procesarPdf(
                PdfRequest.newBuilder().setContenido(ByteString.copyFromUtf8("no es pdf")).build()));
        assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(e.getStatus().getDescription()).isEqualTo("pdf_ilegible");
        assertThat(registros.countByResultado("pdf_ilegible")).isEqualTo(antes + 1);
    }

    @Test
    void clasificaEnLote() {
        List<String> estados = cliente().clasificarValores(ClasificarRequest.newBuilder()
                .addValores(ValorParaClasificar.newBuilder().setValor("112").setRangoReferencia("70 - 100"))
                .addValores(ValorParaClasificar.newBuilder().setValor("36").setRangoReferencia("> 40"))
                .addValores(ValorParaClasificar.newBuilder().setValor("90").setRangoReferencia(""))
                .build()).getEstadosList();
        assertThat(estados).containsExactly("alto", "bajo", "sin_rango");
    }
}
