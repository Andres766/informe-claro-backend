package co.edu.ucc.informeclaro.informes.service;

import co.edu.ucc.informeclaro.comun.error.ReglaNegocioException;
import co.edu.ucc.informeclaro.informes.dto.InformeDto;
import co.edu.ucc.informeclaro.informes.dto.InformeResumenDto;
import co.edu.ucc.informeclaro.informes.dto.NuevoInformeDto;
import co.edu.ucc.informeclaro.informes.dto.PreguntaDto;
import co.edu.ucc.informeclaro.informes.dto.ValorDto;
import co.edu.ucc.informeclaro.informes.model.InformeCargado;
import co.edu.ucc.informeclaro.informes.model.PreguntaSugerida;
import co.edu.ucc.informeclaro.informes.model.ValorExamen;
import co.edu.ucc.informeclaro.informes.repository.InformeRepository;
import io.grpc.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Casos de uso del historial. Las conversiones a DTO ocurren dentro de la transacción. */
@Service
public class InformeService {

    private final InformeRepository repositorio;

    public InformeService(InformeRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public InformeDto guardar(NuevoInformeDto nuevo) {
        InformeCargado informe = new InformeCargado(nuevo.usuarioId(), nuevo.nombreArchivo(), nuevo.resumen(),
                nuevo.motor(), nuevo.idioma());
        nuevo.valores().forEach(v -> informe.agregarValor(new ValorExamen(v.nombreValor(), v.unidad(), v.valor(),
                v.rangoReferencia(), v.estado(), v.explicacionIa())));
        nuevo.preguntas().forEach(p -> informe.agregarPregunta(new PreguntaSugerida(p)));
        return aDto(repositorio.save(informe));
    }

    @Transactional(readOnly = true)
    public List<InformeResumenDto> listar(long usuarioId) {
        return repositorio.findByUsuarioIdOrderByFechaCargaDescIdDesc(usuarioId).stream()
                .map(i -> new InformeResumenDto(i.getId(), i.getNombreArchivo(), i.getFechaCarga(), i.getMotor(),
                        i.getIdioma(), i.getValores().size(),
                        (int) i.getValores().stream().filter(ValorExamen::fueraDeRango).count()))
                .toList();
    }

    @Transactional(readOnly = true)
    public InformeDto obtener(long id, long usuarioId) {
        return aDto(buscar(id, usuarioId));
    }

    @Transactional
    public void eliminar(long id, long usuarioId) {
        repositorio.delete(buscar(id, usuarioId));
    }

    private InformeCargado buscar(long id, long usuarioId) {
        return repositorio.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ReglaNegocioException(Status.Code.NOT_FOUND, "informe_no_encontrado"));
    }

    private static InformeDto aDto(InformeCargado i) {
        return new InformeDto(i.getId(), i.getNombreArchivo(), i.getFechaCarga(), i.getMotor(), i.getIdioma(), i.getResumen(),
                i.getValores().stream().map(v -> new ValorDto(v.getId(), v.getNombreValor(), v.getUnidad(), v.getValor(),
                        v.getRangoReferencia(), v.getEstado(), v.getExplicacionIa())).toList(),
                i.getPreguntas().stream().map(p -> new PreguntaDto(p.getId(), p.getTextoPregunta())).toList());
    }
}
