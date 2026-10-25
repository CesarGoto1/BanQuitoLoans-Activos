package com.grupob.bancobanquito.otorgamiento.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    /**
     * Consulta las solicitudes filtradas por su estado, ordenadas cronológicamente
     * por fecha y hora de registro de forma ascendente (antigüedad).
     * Requisito Funcional RF-04.
     *
     * @param estadoSolicitud Estado a consultar (ej. REGISTRADA).
     * @return Lista de solicitudes ordenadas por antigüedad ascendente.
     */
    List<Solicitud> findByEstadoSolicitudOrderByFechaHoraRegistroAsc(EstadoSolicitud estadoSolicitud);
}
