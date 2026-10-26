package com.grupob.bancobanquito.originacion.internal;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    Optional<Solicitud> findByIdAndStatus(Integer id, EstadoSolicitud status);

    boolean existsByIdClienteAndStatus(Integer idCliente, EstadoSolicitud status);

    Page<Solicitud> findByIdCliente(Integer idCliente, Pageable paginacion);

    Page<Solicitud> findByIdClienteAndStatus(Integer idCliente, EstadoSolicitud status, Pageable paginacion);

    List<Solicitud> findByStatusOrderByFechaHoraRegistroAsc(EstadoSolicitud status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select solicitud from Solicitud solicitud where solicitud.id = :idSolicitud")
    Optional<Solicitud> findForDecision(@Param("idSolicitud") Integer idSolicitud);

    @Query("select solicitud from Solicitud solicitud where solicitud.status = :status "
            + "and not exists (select prestamo from PrestamoCliente prestamo where prestamo.idSolicitud = solicitud.id)")
    Page<Solicitud> findWithoutLoan(@Param("status") EstadoSolicitud status, Pageable paginacion);
}
