package com.grupob.bancobanquito.loans.internal;

import com.grupob.bancobanquito.loans.ConsultaPrestamoService;
import com.grupob.bancobanquito.loans.EstadoCartera;
import com.grupob.bancobanquito.loans.ResumenCuota;
import com.grupob.bancobanquito.loans.ResumenPrestamo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ConsultaPrestamoServiceImpl implements ConsultaPrestamoService {

    private final LoanRepository prestamoRepository;
    private final InstallmentRepository cuotaRepository;

    ConsultaPrestamoServiceImpl(LoanRepository prestamoRepository, InstallmentRepository cuotaRepository) {
        this.prestamoRepository = prestamoRepository;
        this.cuotaRepository = cuotaRepository;
    }

    @Override
    public ResumenPrestamo buscarPorNumeroOperacion(String numeroOperacion) {
        return resumen(prestamoExistente(numeroOperacion));
    }

    @Override
    public Page<ResumenPrestamo> buscarPorCliente(String cedulaCliente, EstadoCartera estadoCartera,
                                                  Pageable paginacion) {
        Page<Loan> prestamos = estadoCartera == null
                ? prestamoRepository.findByCustomerNationalId(cedulaCliente, paginacion)
                : prestamoRepository.findByCustomerNationalIdAndPortfolioStatus(cedulaCliente, estadoCartera, paginacion);
        return prestamos.map(this::resumen);
    }

    @Override
    public Page<ResumenCuota> buscarTablaAmortizacion(String numeroOperacion, Pageable paginacion) {
        return cuotaRepository.findByLoanId(prestamoExistente(numeroOperacion).getId(), paginacion)
                .map(this::resumen);
    }

    private Loan prestamoExistente(String numeroOperacion) {
        return prestamoRepository.findByLoanNumber(numeroOperacion)
                .orElseThrow(() -> new LoanException(HttpStatus.NOT_FOUND,
                        "La operación " + numeroOperacion + " no existe."));
    }

    private ResumenPrestamo resumen(Loan prestamo) {
        return new ResumenPrestamo(prestamo.getLoanNumber(), prestamo.getApplicationId(),
                prestamo.getCustomerNationalId(), prestamo.getApprovedAmount(), prestamo.getEffectiveAnnualRate(),
                prestamo.getTermMonths(), prestamo.getAmortizationMethod(), prestamo.getDisbursementDate(),
                prestamo.getPrincipalBalance(), prestamo.getDaysPastDue(), prestamo.getPortfolioStatus());
    }

    private ResumenCuota resumen(Installment cuota) {
        return new ResumenCuota(cuota.getInstallmentNumber(), cuota.getDueDate(), cuota.getPrincipalAmount(),
                cuota.getInterestAmount(), cuota.getInsuranceAmount(), cuota.getTotalAmount(),
                cuota.getRemainingPrincipalBalance(), cuota.getStatus().name());
    }
}
