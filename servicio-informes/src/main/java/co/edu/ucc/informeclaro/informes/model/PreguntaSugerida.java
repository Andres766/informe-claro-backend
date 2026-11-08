package co.edu.ucc.informeclaro.informes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Pregunta sugerida para llevarle al médico. */
@Entity
@Table(name = "pregunta_sugerida")
public class PreguntaSugerida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "informe_id", nullable = false)
    private InformeCargado informe;

    @Column(name = "texto_pregunta", nullable = false, columnDefinition = "text")
    private String textoPregunta;

    protected PreguntaSugerida() {
    }

    public PreguntaSugerida(String textoPregunta) {
        this.textoPregunta = textoPregunta;
    }

    void asignarInforme(InformeCargado informe) {
        this.informe = informe;
    }

    public Long getId() {
        return id;
    }

    public String getTextoPregunta() {
        return textoPregunta;
    }
}
