package com.grupob.bancobanquito.loans.internal.services;
import com.grupob.bancobanquito.loans.internal.entities.*;
import com.grupob.bancobanquito.loans.internal.repositories.*;
import com.grupob.bancobanquito.loans.internal.services.*;
import com.grupob.bancobanquito.loans.internal.controllers.*;
import com.grupob.bancobanquito.loans.internal.exceptions.*;
import com.grupob.bancobanquito.loans.InstallmentSummary;
import com.grupob.bancobanquito.loans.LoanOperationSummary;
import com.grupob.bancobanquito.loans.LoanOperationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanOperationServiceImpl implements LoanOperationService {

    private static final Logger log = LoggerFactory.getLogger(LoanOperationServiceImpl.class);

    private final LoanOperationRepository operacionRepository;
    private final InstallmentRepository cuotaRepository;

    public LoanOperationServiceImpl(LoanOperationRepository operacionRepository, InstallmentRepository cuotaRepository) {
        this.operacionRepository = operacionRepository;
        this.cuotaRepository = cuotaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanOperationSummary> findByClient(String cedula) {
        log.info("Consulta de operaciones del cliente {}", maskIdentification(cedula));
        return operacionRepository.findByCedulaClienteOrderByFechaDesembolsoDescIdOperacionDesc(cedula).stream()
                .map(LoanOperationServiceImpl::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentSummary> findInstallments(String cedula, String numeroOperacion) {
        log.info("Consulta de cuotas de la operación {} del cliente {}", numeroOperacion, maskIdentification(cedula));
        LoanOperation operacion = operacionRepository.findByNumeroOperacionAndCedulaCliente(numeroOperacion, cedula)
                .orElseThrow(() -> new LoanOperationNotFoundException("No se encontró la operación indicada."));
        return cuotaRepository.findByIdOperacionOrderByNumeroCuotaAsc(operacion.getIdLoanOperation()).stream()
                .map(LoanOperationServiceImpl::toSummary)
                .toList();
    }

    private static LoanOperationSummary toSummary(LoanOperation o) {
        return new LoanOperationSummary(
                o.getIdLoanOperation(),
                o.getIdLoanRequest(),
                o.getNumeroLoanOperation(),
                o.getAmortizationSystem().name(),
                o.getMontoAprobado(),
                o.getTeaAsignada(),
                o.getPlazoMeses(),
                o.getSeguroMensual(),
                o.getContribucionSolca(),
                o.getMontoNeto(),
                o.getCuentaDestino(),
                o.getFechaDesembolso(),
                o.getDisbursementInstructionStatus().name(),
                o.getPortfolioStatus().name(),
                o.getSaldoCapital(),
                o.getDiasMora());
    }

    private static InstallmentSummary toSummary(Installment c) {
        return new InstallmentSummary(
                c.getNumeroInstallment(),
                c.getFechaVencimiento(),
                c.getValorCapital(),
                c.getValorInteres(),
                c.getValorSeguro(),
                c.getTotalInstallment(),
                c.getSaldoCapitalPosterior(),
                c.getCapitalPagado(),
                c.getInteresPagado(),
                c.getSeguroPagado(),
                c.getMoraPagada(),
                c.getMoraAcumulada(),
                c.getInstallmentStatus().name(),
                c.getFechaPago());
    }

    private static String maskIdentification(String cedula) {
        return cedula != null && cedula.length() == 10
                ? cedula.substring(0, 2) + "******" + cedula.substring(8)
                : "**********";
    }
}
















