package co.edu.ucc.informeclaro.documentos.controller;

import co.edu.ucc.informeclaro.documentos.dto.DocumentoDto;
import co.edu.ucc.informeclaro.documentos.dto.ValorExtraidoDto;
import co.edu.ucc.informeclaro.documentos.dto.ValorParaClasificarDto;
import co.edu.ucc.informeclaro.documentos.service.ClasificadorService;
import co.edu.ucc.informeclaro.documentos.service.DocumentoService;
import co.edu.ucc.informeclaro.documentos.service.ExtractorValoresService;
import org.springframework.stereotype.Component;

import java.util.List;

/** Controlador interno: recibe DTOs y coordina los servicios del microservicio. */
@Component
public class DocumentoController {

    private final DocumentoService documentos;
    private final ExtractorValoresService extractor;
    private final ClasificadorService clasificador;

    public DocumentoController(DocumentoService documentos, ExtractorValoresService extractor,
                               ClasificadorService clasificador) {
        this.documentos = documentos;
        this.extractor = extractor;
        this.clasificador = clasificador;
    }

    public DocumentoDto procesarPdf(byte[] contenido) {
        return documentos.procesar(contenido);
    }

    public List<ValorExtraidoDto> extraerValores(String texto) {
        return extractor.extraer(texto);
    }

    public List<String> clasificar(List<ValorParaClasificarDto> valores) {
        return valores.stream().map(v -> clasificador.clasificar(v.valor(), v.rangoReferencia())).toList();
    }
}
