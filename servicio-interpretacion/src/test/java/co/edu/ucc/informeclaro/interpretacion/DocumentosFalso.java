package co.edu.ucc.informeclaro.interpretacion;

import co.edu.ucc.informeclaro.interpretacion.cliente.ClienteDocumentos;
import co.edu.ucc.informeclaro.interpretacion.dto.RangoDto;
import co.edu.ucc.informeclaro.interpretacion.dto.ValorBaseDto;

import java.util.List;

/** Sustituto del microservicio de documentos con los 12 valores del primer informe de ejemplo. */
public class DocumentosFalso implements ClienteDocumentos {

    public static final List<ValorBaseDto> VALORES = List.of(
            new ValorBaseDto("Glucosa en ayunas", "112", "mg/dL", "70 - 100", "alto"),
            new ValorBaseDto("Colesterol total", "215", "mg/dL", "< 200", "alto"),
            new ValorBaseDto("Colesterol HDL", "48", "mg/dL", "> 40", "normal"),
            new ValorBaseDto("Colesterol LDL", "138", "mg/dL", "< 130", "alto"),
            new ValorBaseDto("Triglicéridos", "145", "mg/dL", "< 150", "normal"),
            new ValorBaseDto("Creatinina", "0.9", "mg/dL", "0.6 - 1.2", "normal"),
            new ValorBaseDto("Ácido úrico", "5.1", "mg/dL", "3.5 - 7.2", "normal"),
            new ValorBaseDto("Hemoglobina", "11.8", "g/dL", "12.0 - 16.0", "bajo"),
            new ValorBaseDto("Hematocrito", "36.5", "%", "36 - 46", "normal"),
            new ValorBaseDto("Leucocitos", "7.2", "10^3/uL", "4.5 - 11.0", "normal"),
            new ValorBaseDto("Plaquetas", "250", "10^3/uL", "150 - 450", "normal"),
            new ValorBaseDto("TSH", "2.4", "uUI/mL", "0.4 - 4.0", "normal"));

    @Override
    public List<ValorBaseDto> extraerValores(String texto) {
        return VALORES;
    }

    @Override
    public List<String> clasificar(List<RangoDto> valores) {
        return valores.stream().map(v -> v.rangoReferencia().isBlank() ? "sin_rango" : "normal").toList();
    }
}
