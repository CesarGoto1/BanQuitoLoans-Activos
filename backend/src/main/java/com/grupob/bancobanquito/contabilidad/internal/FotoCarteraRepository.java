package com.grupob.bancobanquito.contabilidad.internal;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FotoCarteraRepository extends JpaRepository<FotoCartera, FotoCarteraId> {

    @Query("""
            select f from FotoCartera f
            where f.prestamo.id = :prestamoId and f.date < :date
            order by f.date desc
            """)
    List<FotoCartera> findPrevious(@Param("prestamoId") Integer prestamoId,
                                   @Param("date") LocalDate date);
}
