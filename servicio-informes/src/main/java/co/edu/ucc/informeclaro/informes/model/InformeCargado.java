package co.edu.ucc.informeclaro.informes.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Informe subido por un usuario. El PDF original NO se guarda: solo los valores extraídos y la
 * explicación. {@code usuarioId} referencia al microservicio de usuarios sin acoplar las entidades.
 */
@Entity
@Table(name = "informe_cargado")
public class InformeCargado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "fecha_carga", nullable = false)
    private Instant fechaCarga;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(nullable = false, columnDefinition = "text")
    private String resumen;

    @Column(nullable = false, length = 40)
    private String motor;

    @Column(length = 2)
    private String idioma;

    @OneToMany(mappedBy = "informe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ValorExamen> valores = new ArrayList<>();

    @OneToMany(mappedBy = "informe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PreguntaSugerida> preguntas = new ArrayList<>();

    protected InformeCargado() {
    }

    public InformeCargado(Long usuarioId, String nombreArchivo, String resumen, String motor, String idioma) {
        this.usuarioId = usuarioId;
        this.fechaCarga = Instant.now();
        this.nombreArchivo = nombreArchivo;
        this.resumen = resumen;
        this.motor = motor;
        this.idioma = idioma;
    }

    public void agregarValor(ValorExamen valor) {
        valor.asignarInforme(this);
        valores.add(valor);
    }

    public void agregarPregunta(PreguntaSugerida pregunta) {
        pregunta.asignarInforme(this);
        preguntas.add(pregunta);
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Instant getFechaCarga() {
        return fechaCarga;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getResumen() {
        return resumen;
    }

    public String getMotor() {
        return motor;
    }

    /** Los informes anteriores a la columna de idioma se generaron en español. */
    public String getIdioma() {
        return idioma == null || idioma.isBlank() ? "es" : idioma;
    }

    public List<ValorExamen> getValores() {
        return valores;
    }

    public List<PreguntaSugerida> getPreguntas() {
        return preguntas;
    }
}
