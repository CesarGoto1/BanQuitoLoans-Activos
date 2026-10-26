package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.prestamos.ConsultaPrestamoService;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.ResumenCuota;
import com.grupob.bancobanquito.prestamos.ResumenPrestamo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ConsultaPrestamoServiceImpl implements ConsultaPrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final CuotaRepository cuotaRepository;

    ConsultaPrestamoServiceImpl(PrestamoRepository prestamoRepository, CuotaRepository cuotaRepository) {
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
        Page<Prestamo> prestamos = estadoCartera == null
                ? prestamoRepository.findByCedulaCliente(cedulaCliente, paginacion)
                : prestamoRepository.findByCedulaClienteAndPortfolioStatus(cedulaCliente, estadoCartera, paginacion);
        return prestamos.map(this::resumen);
    }

    @Override
    public Page<ResumenCuota> buscarTablaAmortizacion(String numeroOperacion, Pageable paginacion) {
        return cuotaRepository.findByLoanId(prestamoExistente(numeroOperacion).getId(), paginacion)
                .map(this::resumen);
    }

    private Prestamo prestamoExistente(String numeroOperacion) {
        return prestamoRepository.findByNumeroOperacion(numeroOperacion)
                .orElseThrow(() -> new ExcepcionPrestamo(HttpStatus.NOT_FOUND,
                        "La operación " + numeroOperacion + " no existe."));
    }

    private ResumenPrestamo resumen(Prestamo prestamo) {
        return new ResumenPrestamo(prestamo.getLoanNumber(), prestamo.getApplicationId(),
                prestamo.getCustomerNationalId(), prestamo.getApprovedAmount(), prestamo.getEffectiveAnnualRate(),
                prestamo.getTermMonths(), prestamo.getAmortizationMethod(), prestamo.getDisbursementDate(),
                prestamo.getPrincipalBalance(), prestamo.getDaysPastDue(), prestamo.getPortfolioStatus());
    }

    private ResumenCuota resumen(Cuota cuota) {
        return new ResumenCuota(cuota.getInstallmentNumber(), cuota.getDueDate(), cuota.getPrincipalAmount(),
                cuota.getInterestAmount(), cuota.getInsuranceAmount(), cuota.getTotalAmount(),
                cuota.getRemainingPrincipalBalance(), cuota.getStatus().name());
    }
}
