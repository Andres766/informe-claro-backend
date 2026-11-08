package co.edu.ucc.informeclaro.infraestructura.controller;

import co.edu.ucc.informeclaro.infraestructura.dto.InformeDetalleOut;
import co.edu.ucc.informeclaro.infraestructura.dto.InformeResumenOut;
import co.edu.ucc.informeclaro.infraestructura.i18n.Idiomas;
import co.edu.ucc.informeclaro.infraestructura.service.AutenticacionService;
import co.edu.ucc.informeclaro.infraestructura.service.OrquestadorInformesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** REST para el navegador: subir, listar, ver y eliminar informes. Orquesta por gRPC. */
@RestController
@RequestMapping("/api/informes")
public class InformesController {

    private final AutenticacionService autenticacion;
    private final OrquestadorInformesService orquestador;
    private final String idiomaPorDefecto;

    public InformesController(AutenticacionService autenticacion, OrquestadorInformesService orquestador,
                              @Value("${informeclaro.idioma-por-defecto}") String idiomaPorDefecto) {
        this.autenticacion = autenticacion;
        this.orquestador = orquestador;
        this.idiomaPorDefecto = idiomaPorDefecto;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InformeDetalleOut subir(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                   @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
                                   @RequestPart("archivo") MultipartFile archivo,
                                   @RequestParam(value = "idioma", required = false) String idioma) throws IOException {
        long usuario = autenticacion.usuarioActual(autorizacion).getId();
        return orquestador.subir(usuario, archivo.getOriginalFilename(), archivo.getContentType(), archivo.getBytes(),
                idioma, Idiomas.normalizar(acceptLanguage, idiomaPorDefecto));
    }

    @GetMapping
    public List<InformeResumenOut> listar(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        return orquestador.listar(autenticacion.usuarioActual(autorizacion).getId());
    }

    @GetMapping("/{id}")
    public InformeDetalleOut ver(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                 @PathVariable long id) {
        return orquestador.obtener(id, autenticacion.usuarioActual(autorizacion).getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                         @PathVariable long id) {
        orquestador.eliminar(id, autenticacion.usuarioActual(autorizacion).getId());
    }
}
