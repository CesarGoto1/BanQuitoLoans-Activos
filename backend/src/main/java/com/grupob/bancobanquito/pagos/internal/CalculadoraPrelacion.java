package com.grupob.bancobanquito.pagos.internal;

import com.grupob.bancobanquito.prestamos.CuotaPorPagar;
import com.grupob.bancobanquito.prestamos.ImputacionCuota;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class CalculadoraPrelacion {

    DistribucionPago distribuir(List<CuotaPorPagar> cuotasPendientes, BigDecimal monto, LocalDate fechaPago) {
        List<CuotaPorPagar> cuotas = cuotasPendientes.stream()
                .sorted(Comparator.comparing(CuotaPorPagar::numeroCuota))
                .toList();
        Integer cuotaEnCurso = cuotas.stream()
                .filter(cuota -> !cuota.fechaVencimiento().isBefore(fechaPago))
                .map(CuotaPorPagar::numeroCuota)
                .findFirst()
                .orElse(Integer.MAX_VALUE);

        List<ImputacionCuota> imputaciones = new ArrayList<>();
        BigDecimal restante = monto;
        BigDecimal seguro = BigDecimal.ZERO;
        BigDecimal mora = BigDecimal.ZERO;
        BigDecimal interes = BigDecimal.ZERO;
        BigDecimal capital = BigDecimal.ZERO;
        BigDecimal anticipado = BigDecimal.ZERO;
        boolean cubreTodaLaDeuda = true;

        for (CuotaPorPagar cuota : cuotas) {
            BigDecimal aSeguro = cuota.seguroPendiente().min(restante);
            restante = restante.subtract(aSeguro);
            BigDecimal aMora = cuota.moraPendiente().min(restante);
            restante = restante.subtract(aMora);
            BigDecimal aInteres = cuota.interesPendiente().min(restante);
            restante = restante.subtract(aInteres);
            BigDecimal aCapital = cuota.capitalPendiente().min(restante);
            restante = restante.subtract(aCapital);

            BigDecimal aplicado = aSeguro.add(aMora).add(aInteres).add(aCapital);
            BigDecimal pendiente = cuota.seguroPendiente().add(cuota.moraPendiente())
                    .add(cuota.interesPendiente()).add(cuota.capitalPendiente());
            if (aplicado.compareTo(pendiente) < 0) {
                cubreTodaLaDeuda = false;
            }
            if (aplicado.signum() == 0) {
                continue;
            }

            imputaciones.add(new ImputacionCuota(cuota.idCuota(), aSeguro, aMora, aInteres, aCapital));
            if (cuota.numeroCuota() > cuotaEnCurso) {
                anticipado = anticipado.add(aplicado);
            } else {
                seguro = seguro.add(aSeguro);
                mora = mora.add(aMora);
                interes = interes.add(aInteres);
                capital = capital.add(aCapital);
            }
        }

        return new DistribucionPago(imputaciones, seguro, mora, interes, capital, anticipado,
                cubreTodaLaDeuda ? restante : BigDecimal.ZERO, cubreTodaLaDeuda);
    }
}
