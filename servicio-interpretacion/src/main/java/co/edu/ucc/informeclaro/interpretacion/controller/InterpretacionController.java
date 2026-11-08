package co.edu.ucc.informeclaro.interpretacion.controller;

import co.edu.ucc.informeclaro.interpretacion.dto.InterpretacionDto;
import co.edu.ucc.informeclaro.interpretacion.dto.SolicitudInterpretacionDto;
import co.edu.ucc.informeclaro.interpretacion.service.InterpretacionService;
import org.springframework.stereotype.Component;

/** Controlador interno: recibe DTOs y delega en el caso de uso de interpretación. */
@Component
public class InterpretacionController {

    private final InterpretacionService servicio;

    public InterpretacionController(InterpretacionService servicio) {
        this.servicio = servicio;
    }

    public InterpretacionDto interpretar(SolicitudInterpretacionDto solicitud) {
        return servicio.interpretar(solicitud.texto(), solicitud.idioma());
    }

    public String aviso(String idioma) {
        return servicio.aviso(idioma);
    }

    public String motor() {
        return servicio.motorActual();
    }
}
