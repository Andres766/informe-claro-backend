package co.edu.ucc.informeclaro.documentos.service;

import co.edu.ucc.informeclaro.comun.error.ReglaNegocioException;
import co.edu.ucc.informeclaro.documentos.dto.TextoPdfDto;
import io.grpc.Status;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

/** Extrae el texto del PDF en memoria: el archivo nunca se escribe en disco ni se guarda. */
@Service
public class LectorPdfService {

    public TextoPdfDto leer(byte[] contenido) {
        String texto;
        int paginas;
        try (PDDocument pdf = Loader.loadPDF(contenido)) {
            PDFTextStripper extractor = new PDFTextStripper();
            extractor.setSortByPosition(true); // respeta las columnas de la tabla del laboratorio
            extractor.setLineSeparator("\n");
            texto = extractor.getText(pdf);
            paginas = pdf.getNumberOfPages();
        } catch (IOException | RuntimeException e) {
            throw new ReglaNegocioException(Status.Code.INVALID_ARGUMENT, "pdf_ilegible");
        }
        texto = texto.strip();
        if (texto.isEmpty()) {
            throw new ReglaNegocioException(Status.Code.INVALID_ARGUMENT, "pdf_sin_texto");
        }
        return new TextoPdfDto(texto, paginas);
    }
}
