package co.edu.ucc.informeclaro.infraestructura.controller;

import co.edu.ucc.informeclaro.infraestructura.dto.EstadisticasAdminOut;
import co.edu.ucc.informeclaro.infraestructura.service.AdminService;
import co.edu.ucc.informeclaro.infraestructura.service.AutenticacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST para el panel de administración. Solo para cuentas con rol "admin" (403 para las demás). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AutenticacionService autenticacion;
    private final AdminService admin;

    public AdminController(AutenticacionService autenticacion, AdminService admin) {
        this.autenticacion = autenticacion;
        this.admin = admin;
    }

    @GetMapping("/estadisticas")
    public EstadisticasAdminOut estadisticas(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        autenticacion.administradorActual(autorizacion);
        return admin.estadisticas();
    }
}
