package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.prestamos.CuotaPorPagar;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.ImputacionCuota;
import com.grupob.bancobanquito.prestamos.OperacionPorPagar;
import com.grupob.bancobanquito.prestamos.ServiceCartera;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ServiceCarteraImpl implements ServiceCartera {

    private static final String USUARIO_AUDITORIA = "system";

    private final PrestamoRepository prestamoRepository;
    private final CuotaRepository cuotaRepository;

    ServiceCarteraImpl(PrestamoRepository prestamoRepository, CuotaRepository cuotaRepository) {
        this.prestamoRepository = prestamoRepository;
        this.cuotaRepository = cuotaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OperacionPorPagar> buscarOperacionPorPagar(String numeroOperacion) {
        return prestamoRepository.findByNumeroOperacion(numeroOperacion).map(prestamo -> new OperacionPorPagar(
                prestamo.getId(), prestamo.getLoanNumber(), prestamo.getCustomerNationalId(),
                prestamo.getPortfolioStatus(), prestamo.getPrincipalBalance(),
                cuotasPendientes(prestamo.getId()).stream().map(this::cuotaPorPagar).toList()));
    }

    @Override
    @Transactional
    public EstadoCartera aplicarImputacion(Integer idOperacion, LocalDate fechaPago,
                                           List<ImputacionCuota> imputaciones) {
        Prestamo prestamo = prestamoRepository.findById(idOperacion)
                .orElseThrow(() -> new IllegalArgumentException("La operación " + idOperacion + " no existe."));
        LocalDateTime ahora = LocalDateTime.now();
        BigDecimal capitalAplicado = BigDecimal.ZERO;

        for (ImputacionCuota imputacion : imputaciones) {
            Cuota cuota = cuotaRepository.findById(imputacion.idCuota())
                    .filter(encontrada -> encontrada.getLoan().getId().equals(idOperacion))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La cuota " + imputacion.idCuota() + " no pertenece a la operación " + idOperacion + "."));
            aplicar(cuota, imputacion, fechaPago, ahora);
            capitalAplicado = capitalAplicado.add(imputacion.capital());
        }
        cuotaRepository.flush();

        BigDecimal saldo = prestamo.getPrincipalBalance().subtract(capitalAplicado);
        if (saldo.signum() < 0) {
            throw new IllegalStateException("El capital aplicado supera el saldo de la operación " + idOperacion + ".");
        }
        prestamo.setPrincipalBalance(saldo);
        if (cuotasPendientes(idOperacion).isEmpty()) {
            prestamo.setPrincipalBalance(BigDecimal.ZERO);
            prestamo.setPortfolioStatus(EstadoCartera.CANCELADA);
            prestamo.setDaysPastDue(0);
        }
        prestamo.setLastModifiedBy(USUARIO_AUDITORIA);
        prestamo.setLastModifiedAt(ahora);
        return prestamo.getPortfolioStatus();
    }

    private List<Cuota> cuotasPendientes(Integer idOperacion) {
        return cuotaRepository.findByLoanIdAndStatusNotOrderByNumeroCuotaAsc(idOperacion, EstadoCuota.PAGADA);
    }

    private CuotaPorPagar cuotaPorPagar(Cuota cuota) {
        return new CuotaPorPagar(cuota.getId(), cuota.getInstallmentNumber(), cuota.getDueDate(),
                cuota.getInsuranceAmount().subtract(cuota.getInsurancePaid()),
                moraPendiente(cuota),
                cuota.getInterestAmount().subtract(cuota.getInterestPaid()),
                cuota.getPrincipalAmount().subtract(cuota.getPrincipalPaid()));
    }

    private BigDecimal moraPendiente(Cuota cuota) {
        BigDecimal pendiente = cuota.getAccruedDefaultInterest().setScale(2, RoundingMode.HALF_UP)
                .subtract(cuota.getDefaultInterestPaid());
        return pendiente.signum() > 0 ? pendiente : BigDecimal.ZERO.setScale(2);
    }

    private void aplicar(Cuota cuota, ImputacionCuota imputacion, LocalDate fechaPago, LocalDateTime ahora) {
        BigDecimal seguroPagado = cuota.getInsurancePaid().add(imputacion.seguro());
        BigDecimal moraPagada = cuota.getDefaultInterestPaid().add(imputacion.mora());
        BigDecimal interesPagado = cuota.getInterestPaid().add(imputacion.interes());
        BigDecimal capitalPagado = cuota.getPrincipalPaid().add(imputacion.capital());

        if (seguroPagado.compareTo(cuota.getInsuranceAmount()) > 0
                || interesPagado.compareTo(cuota.getInterestAmount()) > 0
                || capitalPagado.compareTo(cuota.getPrincipalAmount()) > 0
                || imputacion.mora().compareTo(moraPendiente(cuota)) > 0) {
            throw new IllegalStateException("La imputación supera lo pendiente de la cuota " + cuota.getId() + ".");
        }

        cuota.setInsurancePaid(seguroPagado);
        cuota.setDefaultInterestPaid(moraPagada);
        cuota.setInterestPaid(interesPagado);
        cuota.setPrincipalPaid(capitalPagado);
        cuota.setStatus(estado(cuota));
        cuota.setPaymentDate(cuota.getStatus() == EstadoCuota.PAGADA ? fechaPago : null);
        cuota.setLastModifiedBy(USUARIO_AUDITORIA);
        cuota.setLastModifiedAt(ahora);
    }

    private EstadoCuota estado(Cuota cuota) {
        if (cuota.getInsurancePaid().add(cuota.getInterestPaid()).add(cuota.getPrincipalPaid()).signum() == 0) {
            return EstadoCuota.PENDIENTE;
        }
        if (cuota.getInsurancePaid().compareTo(cuota.getInsuranceAmount()) == 0
                && cuota.getInterestPaid().compareTo(cuota.getInterestAmount()) == 0
                && cuota.getPrincipalPaid().compareTo(cuota.getPrincipalAmount()) == 0) {
            return EstadoCuota.PAGADA;
        }
        return EstadoCuota.PAGO_PARCIAL;
    }
}
