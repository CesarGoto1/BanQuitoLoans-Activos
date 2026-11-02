package com.grupob.bancobanquito.cierre.internal;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

interface CierreDiaRepository extends JpaRepository<CierreDia, Integer> {

    Optional<CierreDia> findFirstByEstadoCierreNotOrderByFechaNegocioAsc(EstadoCierre estadoCierre);

    Optional<CierreDia> findByFechaNegocio(LocalDate fechaNegocio);

    boolean existsByFechaNegocio(LocalDate fechaNegocio);

    boolean existsByFechaNegocioAndEstadoCierre(LocalDate fechaNegocio, EstadoCierre estadoCierre);

    List<CierreDia> findByEstadoCierre(EstadoCierre estadoCierre);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select cierre from CierreDia cierre where cierre.fechaNegocio = :fechaNegocio")
    Optional<CierreDia> findForClosing(@Param("fechaNegocio") LocalDate fechaNegocio);
}
