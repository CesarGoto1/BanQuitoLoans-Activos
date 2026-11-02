package com.grupob.bancobanquito.prestamos.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PagoRepository extends JpaRepository<Pago, Integer> {

    @EntityGraph(attributePaths = "detalles")
    List<Pago> findByIdOperacionOrderByFechaPagoAscIdAsc(Integer idOperacion);

    boolean existsByIdOperacionAndOrigenAndFechaPago(Integer idOperacion, OrigenPago origen, LocalDate fechaPago);

    @Query("select max(p.fechaPago) from PagoPrestamo p where p.idOperacion = :idOperacion and p.origen = :origen")
    Optional<LocalDate> findUltimaFecha(@Param("idOperacion") Integer idOperacion, @Param("origen") OrigenPago origen);

    @EntityGraph(attributePaths = "detalles")
    List<Pago> findByOrigenAndFechaPago(OrigenPago origen, LocalDate fechaPago);

    @Query("""
            select max(p.fechaPago) from PagoPrestamo p
            where p.idOperacion = :idOperacion and p.origen = :origen and p.fechaPago < :fecha""")
    Optional<LocalDate> findUltimaFechaAntesDe(@Param("idOperacion") Integer idOperacion,
                                               @Param("origen") OrigenPago origen, @Param("fecha") LocalDate fecha);
}
