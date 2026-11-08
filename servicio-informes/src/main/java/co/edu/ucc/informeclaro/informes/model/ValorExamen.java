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

/** Un valor del examen con su estado frente al rango y su explicación en lenguaje sencillo. */
@Entity
@Table(name = "valor_examen")
public class ValorExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "informe_id", nullable = false)
    private InformeCargado informe;

    @Column(name = "nombre_valor", nullable = false, length = 160)
    private String nombreValor;

    @Column(nullable = false, length = 40)
    private String unidad;

    @Column(nullable = false, length = 40)
    private String valor;

    @Column(name = "rango_referencia", nullable = false, length = 80)
    private String rangoReferencia;

    @Column(nullable = false, length = 12)
    private String estado;

    @Column(name = "explicacion_ia", nullable = false, columnDefinition = "text")
    private String explicacionIa;

    protected ValorExamen() {
    }

    public ValorExamen(String nombreValor, String unidad, String valor, String rangoReferencia, String estado,
                       String explicacionIa) {
        this.nombreValor = nombreValor;
        this.unidad = unidad;
        this.valor = valor;
        this.rangoReferencia = rangoReferencia;
        this.estado = estado;
        this.explicacionIa = explicacionIa;
    }

    void asignarInforme(InformeCargado informe) {
        this.informe = informe;
    }

    public boolean fueraDeRango() {
        return "alto".equals(estado) || "bajo".equals(estado);
    }

    public Long getId() {
        return id;
    }

    public String getNombreValor() {
        return nombreValor;
    }

    public String getUnidad() {
        return unidad;
    }

    public String getValor() {
        return valor;
    }

    public String getRangoReferencia() {
        return rangoReferencia;
    }

    public String getEstado() {
        return estado;
    }

    public String getExplicacionIa() {
        return explicacionIa;
    }
}
