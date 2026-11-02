package com.grupob.bancobanquito.cobranza.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TramoCarteraRepository extends JpaRepository<TramoCartera, Integer> {

    @Query("SELECT t FROM TramoCartera t WHERE :fechaNegocio >= t.fechaVigenciaDesde " +
           "AND (t.fechaVigenciaHasta IS NULL OR t.fechaVigenciaHasta > :fechaNegocio) " +
           "ORDER BY t.numeroTramo ASC")
    List<TramoCartera> findTramosVigentes(@Param("fechaNegocio") LocalDate fechaNegocio);

    @Query("SELECT t FROM TramoCartera t WHERE :fechaNegocio >= t.fechaVigenciaDesde " +
           "AND (t.fechaVigenciaHasta IS NULL OR t.fechaVigenciaHasta > :fechaNegocio) " +
           "AND :diasMora >= t.diasMoraDesde " +
           "AND (t.diasMoraHasta IS NULL OR :diasMora <= t.diasMoraHasta)")
    Optional<TramoCartera> findTramoPorDiasMora(@Param("diasMora") int diasMora,
                                                @Param("fechaNegocio") LocalDate fechaNegocio);
}
