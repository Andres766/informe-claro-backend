package co.edu.ucc.informeclaro.infraestructura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PreguntaOut(long id, @JsonProperty("texto_pregunta") String textoPregunta) {
}
