package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.prestamos.CargoMora;
import com.grupob.bancobanquito.prestamos.ConsultaPrestamoService;
import com.grupob.bancobanquito.prestamos.DetallePagoAplicado;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.HistorialOperacion;
import com.grupob.bancobanquito.prestamos.MovimientoEstado;
import com.grupob.bancobanquito.prestamos.PagoAplicado;
import com.grupob.bancobanquito.prestamos.ResumenCuota;
import com.grupob.bancobanquito.prestamos.ResumenPrestamo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import com.grupob.bancobanquito.prestamos.DatosOperacionCartera;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final PagoRepository pagoRepository;
    private final CambioEstadoOperacionRepository cambioEstadoRepository;

    ConsultaPrestamoServiceImpl(PrestamoRepository prestamoRepository, CuotaRepository cuotaRepository,
                                PagoRepository pagoRepository, CambioEstadoOperacionRepository cambioEstadoRepository) {
        this.prestamoRepository = prestamoRepository;
        this.cuotaRepository = cuotaRepository;
        this.pagoRepository = pagoRepository;
        this.cambioEstadoRepository = cambioEstadoRepository;
    }

    @Override
    public List<DatosOperacionCartera> buscarDatosPorIds(Collection<Integer> identificadores) {
        if (identificadores == null || identificadores.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(identificadores).size() != identificadores.size()) {
            throw new ExcepcionPrestamo(HttpStatus.BAD_REQUEST, "Los identificadores de operaciones son inválidos o están duplicados.");
        }
        if (identificadores.isEmpty()) {
            return List.of();
        }
        var solicitados = new HashSet<>(identificadores);
        Map<Integer, DatosOperacionCartera> datos = new HashMap<>();
        for (Prestamo prestamo : prestamoRepository.findAllById(identificadores)) {
            if (prestamo == null || prestamo.getId() == null || !solicitados.contains(prestamo.getId())
                    || datos.containsKey(prestamo.getId()) || prestamo.getCustomerId() == null
                    || prestamo.getCustomerId() <= 0 || prestamo.getApprovedAmount() == null
                    || prestamo.getApprovedAmount().signum() <= 0
                    || prestamo.getApprovedAmount().stripTrailingZeros().scale() > 2
                    || prestamo.getTermMonths() == null || prestamo.getTermMonths() <= 0
                    || prestamo.getAmortizationMethod() == null) {
                throw new ExcepcionPrestamo(HttpStatus.CONFLICT, "Las operaciones consultadas contienen datos originales incompletos o inválidos.");
            }
            datos.put(prestamo.getId(), new DatosOperacionCartera(prestamo.getId(), prestamo.getCustomerId(),
                    prestamo.getApprovedAmount(), prestamo.getTermMonths(), prestamo.getAmortizationMethod()));
        }
        if (datos.size() != identificadores.size()) {
            throw new ExcepcionPrestamo(HttpStatus.NOT_FOUND, "No se encontraron todas las operaciones solicitadas.");
        }
        return identificadores.stream().map(datos::get).toList();
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

    @Override
    public HistorialOperacion buscarHistorial(String numeroOperacion) {
        Prestamo prestamo = prestamoExistente(numeroOperacion);
        List<Cuota> cuotas = cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(prestamo.getId());
        Map<Integer, Integer> numeroPorCuota = cuotas.stream()
                .collect(Collectors.toMap(Cuota::getId, Cuota::getInstallmentNumber));
        List<PagoAplicado> pagos = pagoRepository.findByIdOperacionOrderByFechaPagoAscIdAsc(prestamo.getId()).stream()
                .map(pago -> pagoAplicado(pago, numeroPorCuota))
                .toList();
        List<CargoMora> cargosMora = cuotas.stream()
                .filter(cuota -> cuota.getAccruedDefaultInterest().signum() > 0)
                .map(this::cargoMora)
                .toList();
        List<MovimientoEstado> movimientos = cambioEstadoRepository
                .findByIdOperacionOrderByFechaCambioAscIdAsc(prestamo.getId()).stream()
                .map(cambio -> new MovimientoEstado(cambio.getFechaCambio(), cambio.getEstadoAnterior(),
                        cambio.getEstadoNuevo(), cambio.getDiasMora(), cambio.getUsuarioCreacion()))
                .toList();
        return new HistorialOperacion(prestamo.getLoanNumber(), pagos, cargosMora, movimientos);
    }

    private PagoAplicado pagoAplicado(Pago pago, Map<Integer, Integer> numeroPorCuota) {
        List<DetallePagoAplicado> detalles = pago.getDetalles().stream()
                .map(detalle -> new DetallePagoAplicado(numeroPorCuota.get(detalle.getIdCuota()),
                        detalle.getAplicadoSeguro(), detalle.getAplicadoMora(), detalle.getAplicadoInteres(),
                        detalle.getAplicadoCapital()))
                .toList();
        return new PagoAplicado(pago.getId(), pago.getFechaPago(), pago.getOrigen().name(),
                pago.getModalidadAbono() == null ? null : pago.getModalidadAbono().name(), pago.getMontoRecibido(),
                sumar(detalles, DetallePagoAplicado::aplicadoSeguro), sumar(detalles, DetallePagoAplicado::aplicadoMora),
                sumar(detalles, DetallePagoAplicado::aplicadoInteres),
                sumar(detalles, DetallePagoAplicado::aplicadoCapital), pago.getUsuarioCreacion(),
                pago.getFechaCreacion(), detalles);
    }

    private BigDecimal sumar(List<DetallePagoAplicado> detalles, Function<DetallePagoAplicado, BigDecimal> valor) {
        return detalles.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CargoMora cargoMora(Cuota cuota) {
        BigDecimal generada = cuota.getAccruedDefaultInterest().setScale(2, RoundingMode.HALF_UP);
        BigDecimal pagada = cuota.getDefaultInterestPaid();
        return new CargoMora(cuota.getInstallmentNumber(), cuota.getDueDate(), cuota.getStatus().name(), generada,
                pagada, generada.subtract(pagada).max(BigDecimal.ZERO));
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
