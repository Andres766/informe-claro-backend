package co.edu.ucc.informeclaro.interpretacion.cliente;

import co.edu.ucc.informeclaro.interpretacion.dto.RangoDto;
import co.edu.ucc.informeclaro.interpretacion.dto.ValorBaseDto;

import java.util.List;

/** Lo que este microservicio necesita del microservicio de documentos (comunicación interna: gRPC). */
public interface ClienteDocumentos {

    List<ValorBaseDto> extraerValores(String texto);

    List<String> clasificar(List<RangoDto> valores);
}
