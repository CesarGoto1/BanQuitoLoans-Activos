package com.grupob.bancobanquito.prestamos.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DevengoDiarioRepository extends JpaRepository<DevengoDiario, Integer> {

    boolean existsByPrestamoIdAndFechaDevengo(Integer idPrestamo, LocalDate fechaDevengo);

    // Interés devengado de una operación entre dos fechas, ambas inclusive.
    @Query("""
            select coalesce(sum(d.interesDevengado), 0) from DevengoDiario d
            where d.prestamo.id = :idPrestamo and d.fechaDevengo between :desde and :hasta""")
    BigDecimal sumarInteresDevengado(@Param("idPrestamo") Integer idPrestamo, @Param("desde") LocalDate desde,
                                     @Param("hasta") LocalDate hasta);
}
