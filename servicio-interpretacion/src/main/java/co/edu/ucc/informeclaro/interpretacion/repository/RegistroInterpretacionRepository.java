package co.edu.ucc.informeclaro.interpretacion.repository;

import co.edu.ucc.informeclaro.interpretacion.model.RegistroInterpretacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RegistroInterpretacionRepository extends JpaRepository<RegistroInterpretacion, Long> {

    Optional<RegistroInterpretacion> findTopByOrderByIdDesc();

    /** [motor, interpretaciones, duración promedio en ms] por motor. */
    @Query("select r.motor, count(r), avg(r.duracionMs) from RegistroInterpretacion r group by r.motor")
    List<Object[]> usoPorMotor();

    @Query("select coalesce(avg(r.duracionMs), 0) from RegistroInterpretacion r")
    double duracionPromedio();

    @Query("select coalesce(sum(r.guardrailsActivados), 0) from RegistroInterpretacion r")
    long guardrailsActivados();

    long countByMotor(String motor);

    List<RegistroInterpretacion> findTop5ByCausaRespaldoIsNotNullOrderByIdDesc();
}
