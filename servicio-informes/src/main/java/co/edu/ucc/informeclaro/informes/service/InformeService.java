package co.edu.ucc.informeclaro.informes.service;

import co.edu.ucc.informeclaro.comun.error.ReglaNegocioException;
import co.edu.ucc.informeclaro.informes.dto.EstadisticasInformesDto;
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

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Casos de uso del historial. Las conversiones a DTO ocurren dentro de la transacción. */
@Service
public class InformeService {

    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final int MESES = 12;
    private static final int VALORES_FRECUENTES = 8;

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

    /** Totales, informes por usuario, por mes e idioma, y los valores que más salen fuera de rango. */
    @Transactional(readOnly = true)
    public EstadisticasInformesDto estadisticas() {
        List<EstadisticasInformesDto.PorUsuario> porUsuario = repositorio.contarPorUsuario().stream()
                .map(f -> new EstadisticasInformesDto.PorUsuario((Long) f[0], (Long) f[1], (Instant) f[2]))
                .sorted(Comparator.comparingLong(EstadisticasInformesDto.PorUsuario::informes).reversed())
                .toList();

        Map<YearMonth, Long> meses = repositorio.fechasDeCarga().stream()
                .collect(Collectors.groupingBy(f -> YearMonth.from(f.atZone(COLOMBIA)), TreeMap::new, Collectors.counting()));
        List<EstadisticasInformesDto.PorMes> porMes = meses.entrySet().stream()
                .skip(Math.max(0, meses.size() - MESES))
                .map(e -> new EstadisticasInformesDto.PorMes(e.getKey().toString(), e.getValue()))
                .toList();

        List<EstadisticasInformesDto.PorIdioma> porIdioma = repositorio.contarPorIdioma().stream()
                .collect(Collectors.groupingBy(f -> f[0] == null ? "es" : (String) f[0], TreeMap::new,
                        Collectors.summingLong(f -> (Long) f[1])))
                .entrySet().stream()
                .map(e -> new EstadisticasInformesDto.PorIdioma(e.getKey(), e.getValue()))
                .toList();

        List<EstadisticasInformesDto.ValorFrecuente> valores = repositorio.contarPorValor().stream()
                .map(f -> new EstadisticasInformesDto.ValorFrecuente((String) f[0], ((Number) f[1]).longValue(), (Long) f[2]))
                .filter(v -> v.fueraDeRango() > 0)
                .sorted(Comparator.comparingLong(EstadisticasInformesDto.ValorFrecuente::fueraDeRango).reversed()
                        .thenComparing(EstadisticasInformesDto.ValorFrecuente::nombreValor))
                .limit(VALORES_FRECUENTES)
                .toList();

        return new EstadisticasInformesDto(repositorio.count(), repositorio.contarValores(),
                repositorio.contarFueraDeRango(), porUsuario, porMes, porIdioma, valores);
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
