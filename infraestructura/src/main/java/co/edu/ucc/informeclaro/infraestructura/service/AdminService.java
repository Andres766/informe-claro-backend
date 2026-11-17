package co.edu.ucc.informeclaro.infraestructura.service;

import co.edu.ucc.informeclaro.grpc.informes.EstadisticasInformesReply;
import co.edu.ucc.informeclaro.grpc.informes.InformesDeUsuario;
import co.edu.ucc.informeclaro.grpc.interpretacion.EstadisticasInterpretacionReply;
import co.edu.ucc.informeclaro.grpc.usuarios.UsuarioReply;
import co.edu.ucc.informeclaro.infraestructura.cliente.InformesGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.cliente.InterpretacionGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.cliente.UsuariosGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.dto.EstadisticasAdminOut;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Arma el panel de administración pidiendo a cada microservicio sus cifras por gRPC (usuarios,
 * informes e interpretación) y uniéndolas aquí: ningún servicio lee las tablas de otro.
 */
@Service
public class AdminService {

    private final UsuariosGrpcCliente usuarios;
    private final InformesGrpcCliente informes;
    private final InterpretacionGrpcCliente interpretacion;

    public AdminService(UsuariosGrpcCliente usuarios, InformesGrpcCliente informes, InterpretacionGrpcCliente interpretacion) {
        this.usuarios = usuarios;
        this.informes = informes;
        this.interpretacion = interpretacion;
    }

    public EstadisticasAdminOut estadisticas() {
        List<UsuarioReply> cuentas = usuarios.listar();
        EstadisticasInformesReply inf = informes.estadisticas();
        EstadisticasInterpretacionReply ia = interpretacion.estadisticas();

        Map<Long, InformesDeUsuario> porUsuario = inf.getPorUsuarioList().stream()
                .collect(Collectors.toMap(InformesDeUsuario::getUsuarioId, Function.identity()));
        List<EstadisticasAdminOut.Usuario> filas = cuentas.stream()
                .map(u -> {
                    InformesDeUsuario conteo = porUsuario.get(u.getId());
                    return new EstadisticasAdminOut.Usuario(u.getId(), u.getNombre(), u.getEmail(), rol(u),
                            conteo == null ? 0 : conteo.getInformes(), conteo == null ? null : conteo.getUltimoInforme());
                })
                .sorted(Comparator.comparingLong(EstadisticasAdminOut.Usuario::informes).reversed()
                        .thenComparing(EstadisticasAdminOut.Usuario::nombre))
                .toList();

        return new EstadisticasAdminOut(
                new EstadisticasAdminOut.Totales(cuentas.size(), cuentas.stream().filter(u -> "admin".equals(rol(u))).count(),
                        inf.getTotalInformes(), inf.getTotalValores(), inf.getFueraDeRango()),
                filas,
                inf.getPorMesList().stream().map(m -> new EstadisticasAdminOut.PorMes(m.getMes(), m.getInformes())).toList(),
                inf.getPorIdiomaList().stream().map(i -> new EstadisticasAdminOut.PorIdioma(i.getIdioma(), i.getInformes())).toList(),
                new EstadisticasAdminOut.Interpretaciones(ia.getTotal(), ia.getDuracionPromedioMs(),
                        ia.getGuardrailsActivados(), ia.getRespaldo()),
                ia.getMotoresList().stream().map(m -> new EstadisticasAdminOut.Motor(m.getMotor(), m.getInterpretaciones(),
                        m.getDuracionPromedioMs())).toList(),
                inf.getValoresFueraDeRangoList().stream().map(v -> new EstadisticasAdminOut.ValorFrecuente(
                        v.getNombreValor(), v.getFueraDeRango(), v.getTotal())).toList(),
                ia.getFallosRecientesList().stream().map(f -> new EstadisticasAdminOut.Fallo(f.getFecha(), f.getCausa())).toList());
    }

    private static String rol(UsuarioReply u) {
        return u.getRol().isEmpty() ? "usuario" : u.getRol();
    }
}
