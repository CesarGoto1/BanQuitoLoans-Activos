package com.grupob.bancobanquito.reportes.internal;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.reportes.CorteCartera;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

final class CalculosCartera {

    static final BigDecimal CERO = new BigDecimal("0.00");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private CalculosCartera() {
    }

    static void validar(CorteCartera corte) {
        if (corte == null) {
            throw new IllegalArgumentException("El corte de cartera es obligatorio.");
        }
        Map<Integer, CorteCartera.Tramo> tramos = new HashMap<>();
        Set<Integer> numeros = new HashSet<>();
        for (CorteCartera.Tramo tramo : corte.tramos()) {
            if (tramo.idTramo() == null || tramo.idTramo() <= 0 || tramo.numeroTramo() == null
                    || tramo.numeroTramo() <= 0 || tramo.diasMoraDesde() == null || tramo.diasMoraDesde() < 0
                    || (tramo.diasMoraHasta() != null && tramo.diasMoraHasta() < tramo.diasMoraDesde())
                    || tramo.estadoCartera() == null || tramo.estadoCartera() == EstadoCartera.CANCELADA
                    || tramo.porcentajeProvision() == null || tramo.porcentajeProvision().signum() < 0
                    || tramo.porcentajeProvision().compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("La configuración del tramo es inválida.");
            }
            if (tramos.put(tramo.idTramo(), tramo) != null || !numeros.add(tramo.numeroTramo())) {
                throw new IllegalArgumentException("El corte contiene tramos duplicados.");
            }
        }
        Set<Integer> operaciones = new HashSet<>();
        for (CorteCartera.Operacion operacion : corte.operaciones()) {
            if (operacion.idOperacion() == null || operacion.idOperacion() <= 0
                    || !operaciones.add(operacion.idOperacion())) {
                throw new IllegalArgumentException("El identificador de operación es inválido o está duplicado.");
            }
            CorteCartera.Tramo tramo = tramos.get(operacion.idTramo());
            if (tramo == null || operacion.estadoCartera() != tramo.estadoCartera()) {
                throw new IllegalArgumentException("La operación no corresponde a un tramo configurado del corte.");
            }
            validarImporte(operacion.saldoCapital());
            validarImporte(operacion.provisionConstituida());
        }
    }

    static void validarImporte(BigDecimal importe) {
        if (importe == null || importe.signum() < 0 || importe.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Los importes deben ser no negativos y tener máximo dos decimales.");
        }
    }

    static List<CorteCartera.Tramo> tramosOrdenados(CorteCartera corte) {
        return corte.tramos().stream().sorted(Comparator.comparing(CorteCartera.Tramo::numeroTramo)).toList();
    }

    static BigDecimal saldo(List<CorteCartera.Operacion> operaciones) {
        return operaciones.stream().map(CorteCartera.Operacion::saldoCapital).reduce(CERO, BigDecimal::add);
    }

    static BigDecimal provision(List<CorteCartera.Operacion> operaciones) {
        return operaciones.stream().map(CorteCartera.Operacion::provisionConstituida).reduce(CERO, BigDecimal::add);
    }

    static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        return total.signum() == 0 ? null : parte.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
    }

    static List<BigDecimal> participaciones(List<BigDecimal> saldos) {
        BigDecimal total = saldos.stream().reduce(CERO, BigDecimal::add);
        List<BigDecimal> porcentajes = new ArrayList<>();
        if (total.signum() == 0) {
            saldos.forEach(saldo -> porcentajes.add(null));
            return porcentajes;
        }
        List<BigDecimal> restos = new ArrayList<>();
        int asignados = 0;
        for (BigDecimal saldo : saldos) {
            BigDecimal[] division = saldo.multiply(new BigDecimal("10000")).divideAndRemainder(total);
            int unidades = division[0].intValueExact();
            porcentajes.add(BigDecimal.valueOf(unidades, 2));
            restos.add(division[1]);
            asignados += unidades;
        }
        List<Integer> orden = IntStream.range(0, saldos.size()).boxed()
                .sorted(Comparator.<Integer, BigDecimal>comparing(restos::get).reversed()
                        .thenComparingInt(Integer::intValue)).toList();
        for (int i = 0; i < 10000 - asignados; i++) {
            int indice = orden.get(i);
            porcentajes.set(indice, porcentajes.get(indice).add(new BigDecimal("0.01")));
        }
        return porcentajes;
    }
}
