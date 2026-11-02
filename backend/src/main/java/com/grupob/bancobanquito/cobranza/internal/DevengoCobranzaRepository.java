package com.grupob.bancobanquito.cobranza.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface DevengoCobranzaRepository extends JpaRepository<DevengoCobranza, Integer> {

    @Query("SELECT COALESCE(SUM(d.interesDevengado), 0) FROM DevengoCobranza d " +
           "WHERE d.idOperacion = :idOperacion AND d.fechaDevengo <= :fechaNegocio")
    BigDecimal sumInteresDevengadoAcumulado(@Param("idOperacion") Integer idOperacion,
                                           @Param("fechaNegocio") LocalDate fechaNegocio);
}
