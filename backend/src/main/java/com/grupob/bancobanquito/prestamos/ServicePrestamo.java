package com.grupob.bancobanquito.prestamos;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicePrestamo {

    VistaPreviaDesembolso previewDisbursement(Integer idSolicitud);

    ResultadoDesembolso disburse(Integer idSolicitud, String cuentaDestino);

    Optional<ResultadoDesembolso> findDisbursement(Integer idSolicitud);

    Page<DesembolsoPendiente> findPendingDisbursements(Pageable paginacion);
}
