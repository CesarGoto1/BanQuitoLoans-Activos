package com.grupob.bancobanquito.origination.internal.services;
import com.grupob.bancobanquito.origination.internal.entities.*;
import com.grupob.bancobanquito.origination.internal.repositories.*;
import com.grupob.bancobanquito.origination.internal.services.*;
import com.grupob.bancobanquito.origination.internal.controllers.*;
import com.grupob.bancobanquito.origination.internal.exceptions.*;
import com.grupob.bancobanquito.origination.LoanRequestSummary;
import com.grupob.bancobanquito.origination.LoanRequestService;
import com.grupob.bancobanquito.liabilities.CustomerSummary;
import com.grupob.bancobanquito.liabilities.CustomerService;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanRequestServiceImpl implements LoanRequestService {

    private static final Logger log = LoggerFactory.getLogger(LoanRequestServiceImpl.class);

    private final LoanRequestRepository solicitudRepository;
    private final CustomerService clienteService;

    public LoanRequestServiceImpl(LoanRequestRepository solicitudRepository, CustomerService clienteService) {
        this.solicitudRepository = solicitudRepository;
        this.clienteService = clienteService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanRequestSummary> findByClient(String cedula, String estado) {
        LoanRequestStatus filtro = parseStatus(estado);
        CustomerSummary cliente = clienteService.findByIdentification(cedula);
        log.info("Consulta de solicitudes del cliente {}", maskIdentification(cedula));

        List<LoanRequest> solicitudes = filtro == null
                ? solicitudRepository.findByIdClienteOrderByFechaHoraRegistroDesc(cliente.idCliente())
                : solicitudRepository.findByIdClienteAndEstadoSolicitudOrderByFechaHoraRegistroDesc(
                        cliente.idCliente(), filtro);
        return solicitudes.stream().map(LoanRequestServiceImpl::toSummary).toList();
    }

    private static LoanRequestStatus parseStatus(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return LoanRequestStatus.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidLoanRequestStatusException(
                    "El estado indicado no es válido. Use: " + Arrays.toString(LoanRequestStatus.values()) + ".");
        }
    }

    private static LoanRequestSummary toSummary(LoanRequest s) {
        return new LoanRequestSummary(
                s.getIdLoanRequest(),
                s.getAmortizationSystem().name(),
                s.getMontoSolicitado(),
                s.getPlazoMeses(),
                s.getCreditPurpose().name(),
                s.getFechaLoanRequest(),
                s.getFechaHoraRegistro(),
                s.getInstallmentEvaluacionReferencial(),
                s.getTeaAsignada(),
                s.getInstallmentEvaluacionDefinitiva(),
                s.getLoanRequestStatus().name(),
                s.getFechaResolucion(),
                s.getMotivoRechazo());
    }

    private static String maskIdentification(String cedula) {
        return cedula != null && cedula.length() == 10
                ? cedula.substring(0, 2) + "******" + cedula.substring(8)
                : "**********";
    }
}
















