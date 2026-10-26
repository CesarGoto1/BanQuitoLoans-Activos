package com.grupob.bancobanquito.originacion;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServiceSolicitudes {

    Optional<SolicitudAprobada> findApproved(Integer idSolicitud);

    Page<SolicitudDesembolsoPendiente> findPendingDisbursement(Pageable paginacion);
}
