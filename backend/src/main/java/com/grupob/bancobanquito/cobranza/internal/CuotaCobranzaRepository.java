package com.grupob.bancobanquito.cobranza.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CuotaCobranzaRepository extends JpaRepository<CuotaCobranza, Integer> {

    List<CuotaCobranza> findByIdOperacionOrderByNumeroCuotaAsc(Integer idOperacion);

    @Query("SELECT c FROM CuotaCobranza c WHERE c.idOperacion = :idOperacion " +
           "AND c.estadoCuota <> 'PAGADA' " +
           "AND c.fechaVencimiento < :fechaNegocio " +
           "ORDER BY c.fechaVencimiento ASC")
    List<CuotaCobranza> findCuotasVencidasPendientes(@Param("idOperacion") Integer idOperacion,
                                                    @Param("fechaNegocio") LocalDate fechaNegocio);

    @Query("SELECT COALESCE(SUM(c.moraAcumulada - c.moraPagada), 0) FROM CuotaCobranza c " +
           "WHERE c.idOperacion = :idOperacion")
    BigDecimal sumMoraPendientePorOperacion(@Param("idOperacion") Integer idOperacion);

    @Query("SELECT COALESCE(SUM(c.valorInteres - c.interesPagado), 0) FROM CuotaCobranza c " +
           "WHERE c.idOperacion = :idOperacion AND c.estadoCuota <> 'PAGADA'")
    BigDecimal sumInteresPendientePorOperacion(@Param("idOperacion") Integer idOperacion);
}
