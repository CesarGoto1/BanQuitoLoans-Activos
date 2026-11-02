package com.grupob.bancobanquito.pagos.internal;

import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import com.grupob.bancobanquito.pagos.EstadoAplicacionPago;
import com.grupob.bancobanquito.pagos.PagoRecaudacion;
import com.grupob.bancobanquito.pagos.ResultadoPago;
import com.grupob.bancobanquito.pagos.ServicePagos;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.ImputacionCuota;
import com.grupob.bancobanquito.prestamos.OperacionPorPagar;
import com.grupob.bancobanquito.prestamos.ServiceCartera;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ServicePagosImpl implements ServicePagos {

    private static final String USUARIO_AUDITORIA = "system";
    private static final String OPERACION_NO_EXISTE = "REG-003";
    private static final String IDENTIFICACION_NO_CORRESPONDE = "REG-004";
    private static final String MONTO_INVALIDO = "REG-005";
    private static final String OPERACION_CANCELADA = "REG-006";
    private static final String SOBREPAGO = "REG-007";

    private final ServiceCartera servicioCartera;
    private final ServiceContabilidad servicioContabilidad;
    private final CalculadoraPrelacion calculadoraPrelacion;
    private final PagoRecaudacionRepository pagoRecaudacionRepository;
    private final DetallePagoRepository detallePagoRepository;

    ServicePagosImpl(ServiceCartera servicioCartera, ServiceContabilidad servicioContabilidad,
                    CalculadoraPrelacion calculadoraPrelacion, PagoRecaudacionRepository pagoRecaudacionRepository,
                    DetallePagoRepository detallePagoRepository) {
        this.servicioCartera = servicioCartera;
        this.servicioContabilidad = servicioContabilidad;
        this.calculadoraPrelacion = calculadoraPrelacion;
        this.pagoRecaudacionRepository = pagoRecaudacionRepository;
        this.detallePagoRepository = detallePagoRepository;
    }

    @Override
    @Transactional
    public ResultadoPago aplicarPagoRecaudacion(PagoRecaudacion pago) {
        if (pago.idRegistro() == null || pago.fechaNegocio() == null) {
            throw new IllegalArgumentException("El pago debe indicar el registro de recaudación y la fecha de negocio.");
        }
        if (pagoRecaudacionRepository.existsByIdRegistro(pago.idRegistro())) {
            throw new IllegalStateException("El registro de recaudación " + pago.idRegistro() + " ya fue aplicado.");
        }
        if (pago.monto() == null || pago.monto().signum() <= 0 || pago.monto().stripTrailingZeros().scale() > 2) {
            return rechazado(MONTO_INVALIDO);
        }

        Optional<OperacionPorPagar> encontrada = servicioCartera.buscarOperacionPorPagar(pago.numeroOperacion());
        if (encontrada.isEmpty()) {
            return rechazado(OPERACION_NO_EXISTE);
        }
        OperacionPorPagar operacion = encontrada.get();
        if (!operacion.cedulaCliente().equals(pago.cedulaCliente())) {
            return rechazado(IDENTIFICACION_NO_CORRESPONDE);
        }
        if (operacion.estadoCartera() == EstadoCartera.CANCELADA) {
            return rechazado(OPERACION_CANCELADA);
        }

        DistribucionPago distribucion = calculadoraPrelacion.distribuir(operacion.cuotasPendientes(), pago.monto(),
                pago.fechaNegocio());
        servicioCartera.aplicarImputacion(operacion.idOperacion(), pago.fechaNegocio(), distribucion.imputaciones());

        Pago registrado = guardarPago(operacion, pago, distribucion);
        guardarDetalles(registrado, distribucion.imputaciones());
        servicioContabilidad.registrarAsientos(asientos(operacion, pago, distribucion));

        return new ResultadoPago(EstadoAplicacionPago.APLICADO,
                distribucion.excedente().signum() > 0 ? SOBREPAGO : null,
                distribucion.seguro(), distribucion.mora(), distribucion.interes(), distribucion.capital(),
                distribucion.anticipado(), distribucion.excedente());
    }

    private Pago guardarPago(OperacionPorPagar operacion, PagoRecaudacion pago, DistribucionPago distribucion) {
        Pago registrado = new Pago();
        registrado.setIdOperacion(operacion.idOperacion());
        registrado.setIdRegistro(pago.idRegistro());
        registrado.setOrigenPago(OrigenPago.ARCHIVO_RECAUDACION);
        registrado.setFechaPago(pago.fechaNegocio());
        registrado.setExcedenteDevuelto(distribucion.excedente());
        registrado.setUsuarioCreacion(USUARIO_AUDITORIA);
        registrado.setFechaCreacion(LocalDateTime.now());
        return pagoRecaudacionRepository.save(registrado);
    }

    private void guardarDetalles(Pago pago, List<ImputacionCuota> imputaciones) {
        List<DetallePago> detalles = new ArrayList<>();
        for (ImputacionCuota imputacion : imputaciones) {
            DetallePago detalle = new DetallePago();
            detalle.setIdPago(pago.getId());
            detalle.setIdCuota(imputacion.idCuota());
            detalle.setAplicadoSeguro(imputacion.seguro());
            detalle.setAplicadoMora(imputacion.mora());
            detalle.setAplicadoInteres(imputacion.interes());
            detalle.setAplicadoCapital(imputacion.capital());
            detalle.setUsuarioCreacion(USUARIO_AUDITORIA);
            detalle.setFechaCreacion(pago.getFechaCreacion());
            detalles.add(detalle);
        }
        detallePagoRepository.saveAll(detalles);
    }

    private List<MovimientoContable> asientos(OperacionPorPagar operacion, PagoRecaudacion pago,
                                            DistribucionPago distribucion) {
        List<MovimientoContable> movimientos = new ArrayList<>();
        String clave = "PAGO-REG-" + pago.idRegistro() + "-";
        LocalDate fecha = pago.fechaNegocio();

        if (operacion.estadoCartera() == EstadoCartera.CASTIGADA) {
            agregar(movimientos, operacion, fecha, TipoAsiento.RECUPERACION,
                    CodigoCuentaContable.INGRESOS_RECUPERACIONES, distribucion.totalAplicado(), clave + "RECUPERACION");
            return movimientos;
        }

        BigDecimal seguro = BigDecimal.ZERO;
        BigDecimal mora = BigDecimal.ZERO;
        BigDecimal interes = BigDecimal.ZERO;
        BigDecimal capital = BigDecimal.ZERO;
        for (ImputacionCuota imputacion : distribucion.imputaciones()) {
            seguro = seguro.add(imputacion.seguro());
            mora = mora.add(imputacion.mora());
            interes = interes.add(imputacion.interes());
            capital = capital.add(imputacion.capital());
        }
        agregar(movimientos, operacion, fecha, TipoAsiento.PAGO, CodigoCuentaContable.SEGUROS_POR_PAGAR,
                seguro, clave + "SEGURO");
        agregar(movimientos, operacion, fecha, TipoAsiento.PAGO, CodigoCuentaContable.INTERESES_MORA_POR_COBRAR,
                mora, clave + "MORA");
        agregar(movimientos, operacion, fecha, TipoAsiento.PAGO, CodigoCuentaContable.INTERESES_POR_COBRAR,
                interes, clave + "INTERES");
        agregar(movimientos, operacion, fecha, TipoAsiento.PAGO, cuentaCartera(operacion.estadoCartera()),
                capital, clave + "CAPITAL");
        return movimientos;
    }

    private void agregar(List<MovimientoContable> movimientos, OperacionPorPagar operacion, LocalDate fecha,
                        TipoAsiento tipo, CodigoCuentaContable cuentaCredito, BigDecimal monto, String clave) {
        if (monto.signum() > 0) {
            movimientos.add(new MovimientoContable(operacion.idOperacion(), fecha, tipo,
                    CodigoCuentaContable.RECAUDACIONES_POR_APLICAR, cuentaCredito, monto, clave));
        }
    }

    private CodigoCuentaContable cuentaCartera(EstadoCartera estado) {
        return switch (estado) {
            case VENCIDA -> CodigoCuentaContable.CARTERA_VENCIDA;
            case EN_LITIGIO -> CodigoCuentaContable.CARTERA_EN_LITIGIO;
            default -> CodigoCuentaContable.CARTERA_POR_VENCER;
        };
    }

    private ResultadoPago rechazado(String codigo) {
        return new ResultadoPago(EstadoAplicacionPago.RECHAZADO, codigo, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
