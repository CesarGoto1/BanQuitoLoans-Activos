package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.originacion.SolicitudAprobada;
import com.grupob.bancobanquito.originacion.ServiceSolicitudes;
import com.grupob.bancobanquito.originacion.SolicitudDesembolsoPendiente;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ServiceSolicitudesImpl implements ServiceSolicitudes {

    private final SolicitudRepository loanApplicationRepository;

    ServiceSolicitudesImpl(SolicitudRepository loanApplicationRepository) {
        this.loanApplicationRepository = loanApplicationRepository;
    }

    @Override
    public Optional<SolicitudAprobada> findApproved(Integer idSolicitud) {
        return loanApplicationRepository.findByIdAndStatus(idSolicitud, EstadoSolicitud.APROBADA)
                .map(application -> new SolicitudAprobada(application.getId(), application.getCustomerId(),
                        application.getRequestedAmount(), application.getAssignedRate(), application.getTermMonths(),
                        application.getAmortizationMethod()));
    }

    @Override
    public Page<SolicitudDesembolsoPendiente> findPendingDisbursement(Pageable paginacion) {
        return loanApplicationRepository.findWithoutLoan(EstadoSolicitud.APROBADA, paginacion)
                .map(application -> new SolicitudDesembolsoPendiente(application.getId(),
                        application.getCustomerId(), application.getRequestedAmount(), application.getAssignedRate(),
                        application.getTermMonths(), application.getAmortizationMethod(),
                        application.getResolutionDate()));
    }
}
