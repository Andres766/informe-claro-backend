package co.edu.ucc.informeclaro.documentos.service;

import co.edu.ucc.informeclaro.comun.error.ReglaNegocioException;
import co.edu.ucc.informeclaro.documentos.dto.DocumentoDto;
import co.edu.ucc.informeclaro.documentos.dto.TextoPdfDto;
import co.edu.ucc.informeclaro.documentos.model.RegistroProcesamiento;
import co.edu.ucc.informeclaro.documentos.repository.RegistroProcesamientoRepository;
import org.springframework.stereotype.Service;

/** Caso de uso: leer el PDF, anonimizarlo y dejar constancia del procesamiento. */
@Service
public class DocumentoService {

    private final LectorPdfService lector;
    private final AnonimizadorService anonimizador;
    private final RegistroProcesamientoRepository registros;

    public DocumentoService(LectorPdfService lector, AnonimizadorService anonimizador,
                            RegistroProcesamientoRepository registros) {
        this.lector = lector;
        this.anonimizador = anonimizador;
        this.registros = registros;
    }

    public DocumentoDto procesar(byte[] contenido) {
        try {
            TextoPdfDto pdf = lector.leer(contenido);
            String texto = anonimizador.anonimizar(pdf.texto());
            registros.save(new RegistroProcesamiento(pdf.paginas(), texto.length(), "ok"));
            return new DocumentoDto(texto, pdf.paginas());
        } catch (ReglaNegocioException e) {
            registros.save(new RegistroProcesamiento(0, 0, e.clave()));
            throw e;
        }
    }
}
