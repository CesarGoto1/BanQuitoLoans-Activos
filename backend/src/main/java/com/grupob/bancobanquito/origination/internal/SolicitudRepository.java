package com.grupob.bancobanquito.origination.internal;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    List<Solicitud> findByEstadoSolicitudOrderByFechaHoraRegistroAsc(EstadoSolicitud estadoSolicitud);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select application from Solicitud application where application.idSolicitud = :applicationId")
    Optional<Solicitud> findForDecision(@Param("applicationId") Integer applicationId);
}
