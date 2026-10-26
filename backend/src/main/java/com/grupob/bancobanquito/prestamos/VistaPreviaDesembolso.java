package com.grupob.bancobanquito.prestamos;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.pasivos.Cuenta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VistaPreviaDesembolso(
        Integer idSolicitud,
        String cedulaCliente,
        String nombreCliente,
        BigDecimal montoAprobado,
        BigDecimal teaAsignada,
        Integer plazoMeses,
        MetodoAmortizacion metodoAmortizacion,
        BigDecimal seguroMensual,
        BigDecimal contribucionSolca,
        BigDecimal montoNeto,
        LocalDate fechaDesembolso,
        List<Cuenta> cuentas) {
}
