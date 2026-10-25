package com.grupob.bancobanquito.origination.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    List<Solicitud> findByEstadoSolicitudOrderByFechaHoraRegistroAsc(EstadoSolicitud estadoSolicitud);
}
