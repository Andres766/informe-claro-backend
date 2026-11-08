package co.edu.ucc.informeclaro.informes.controller;

import co.edu.ucc.informeclaro.informes.dto.InformeDto;
import co.edu.ucc.informeclaro.informes.dto.InformeResumenDto;
import co.edu.ucc.informeclaro.informes.dto.NuevoInformeDto;
import co.edu.ucc.informeclaro.informes.service.InformeService;
import org.springframework.stereotype.Component;

import java.util.List;

/** Controlador interno: recibe DTOs y delega en los casos de uso del historial. */
@Component
public class InformeController {

    private final InformeService servicio;

    public InformeController(InformeService servicio) {
        this.servicio = servicio;
    }

    public InformeDto guardar(NuevoInformeDto nuevo) {
        return servicio.guardar(nuevo);
    }

    public List<InformeResumenDto> listar(long usuarioId) {
        return servicio.listar(usuarioId);
    }

    public InformeDto obtener(long id, long usuarioId) {
        return servicio.obtener(id, usuarioId);
    }

    public void eliminar(long id, long usuarioId) {
        servicio.eliminar(id, usuarioId);
    }
}
