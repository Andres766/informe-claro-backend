package co.edu.ucc.informeclaro.infraestructura.controller;

import co.edu.ucc.informeclaro.infraestructura.cliente.InterpretacionGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.dto.SaludOut;
import co.edu.ucc.informeclaro.infraestructura.i18n.Idiomas;
import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaludController {

    private final InterpretacionGrpcCliente interpretacion;
    private final String idiomaPorDefecto;

    public SaludController(InterpretacionGrpcCliente interpretacion,
                           @Value("${informeclaro.idioma-por-defecto}") String idiomaPorDefecto) {
        this.interpretacion = interpretacion;
        this.idiomaPorDefecto = idiomaPorDefecto;
    }

    @GetMapping("/api/salud")
    public SaludOut salud() {
        String motor;
        try {
            motor = interpretacion.motor();
        } catch (StatusRuntimeException e) {
            motor = "no disponible";
        }
        return new SaludOut("ok", motor, Idiomas.SOPORTADOS, idiomaPorDefecto);
    }
}
