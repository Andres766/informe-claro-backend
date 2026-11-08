package co.edu.ucc.informeclaro.infraestructura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SaludOut(
        String estado,
        String motor,
        List<String> idiomas,
        @JsonProperty("idioma_por_defecto") String idiomaPorDefecto) {
}
