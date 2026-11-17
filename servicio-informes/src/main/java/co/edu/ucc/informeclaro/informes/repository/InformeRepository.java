package co.edu.ucc.informeclaro.informes.repository;

import co.edu.ucc.informeclaro.informes.model.InformeCargado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

import java.util.List;
import java.util.Optional;

public interface InformeRepository extends JpaRepository<InformeCargado, Long> {

    List<InformeCargado> findByUsuarioIdOrderByFechaCargaDescIdDesc(Long usuarioId);

    /** Solo encuentra el informe si pertenece a ese usuario: cada quien ve solo su historial. */
    Optional<InformeCargado> findByIdAndUsuarioId(Long id, Long usuarioId);

    // --- Cifras agregadas para el panel de administración ---------------------------------

    @Query("select count(v) from ValorExamen v")
    long contarValores();

    @Query("select count(v) from ValorExamen v where v.estado in ('alto', 'bajo')")
    long contarFueraDeRango();

    /** [usuarioId, informes, fecha del último informe] por usuario. */
    @Query("select i.usuarioId, count(i), max(i.fechaCarga) from InformeCargado i group by i.usuarioId")
    List<Object[]> contarPorUsuario();

    /** [idioma, informes]; el idioma puede ser nulo en informes antiguos. */
    @Query("select i.idioma, count(i) from InformeCargado i group by i.idioma")
    List<Object[]> contarPorIdioma();

    @Query("select i.fechaCarga from InformeCargado i")
    List<Instant> fechasDeCarga();

    /** [nombre del valor, veces fuera de rango, veces en total]. */
    @Query("""
            select v.nombreValor, sum(case when v.estado in ('alto', 'bajo') then 1 else 0 end), count(v)
            from ValorExamen v group by v.nombreValor""")
    List<Object[]> contarPorValor();
}
