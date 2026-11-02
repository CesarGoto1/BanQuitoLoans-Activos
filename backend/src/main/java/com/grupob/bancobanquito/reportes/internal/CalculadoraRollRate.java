package com.grupob.bancobanquito.reportes.internal;

import com.grupob.bancobanquito.reportes.CorteCartera;
import com.grupob.bancobanquito.reportes.ResultadoRollRate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class CalculadoraRollRate {

    ResultadoRollRate calcular(CorteCartera inicial, CorteCartera finalCorte) {
        CalculosCartera.validar(inicial);
        CalculosCartera.validar(finalCorte);
        if (!inicial.fechaCorte().isBefore(finalCorte.fechaCorte())) {
            throw new IllegalArgumentException("La fecha inicial debe ser anterior a la fecha final.");
        }
        Map<Integer, CorteCartera.Operacion> finales = finalCorte.operaciones().stream()
                .collect(Collectors.toMap(CorteCartera.Operacion::idOperacion, operacion -> operacion));
        Map<Integer, List<CorteCartera.Operacion>> porTramo = inicial.operaciones().stream()
                .collect(Collectors.groupingBy(CorteCartera.Operacion::idTramo));
        List<CorteCartera.Tramo> destinos = CalculosCartera.tramosOrdenados(finalCorte);
        List<ResultadoRollRate.Fila> filas = new ArrayList<>();
        for (CorteCartera.Tramo tramo : CalculosCartera.tramosOrdenados(inicial)) {
            filas.add(fila(tramo, porTramo.getOrDefault(tramo.idTramo(), List.of()), finales, destinos));
        }
        return new ResultadoRollRate(inicial.fechaCorte(), finalCorte.fechaCorte(), filas,
                inicial.operaciones().size(), CalculosCartera.saldo(inicial.operaciones()));
    }

    private ResultadoRollRate.Fila fila(CorteCartera.Tramo origen, List<CorteCartera.Operacion> operaciones,
                                        Map<Integer, CorteCartera.Operacion> finales,
                                        List<CorteCartera.Tramo> destinos) {
        Map<Integer, Integer> posiciones = new HashMap<>();
        for (int i = 0; i < destinos.size(); i++) {
            posiciones.put(destinos.get(i).idTramo(), i);
        }
        int cancelada = destinos.size();
        List<BigDecimal> saldos = new ArrayList<>();
        long[] cantidades = new long[destinos.size() + 1];
        for (int i = 0; i <= destinos.size(); i++) {
            saldos.add(CalculosCartera.CERO);
        }
        for (CorteCartera.Operacion operacion : operaciones) {
            CorteCartera.Operacion destino = finales.get(operacion.idOperacion());
            int indice = destino == null ? cancelada : posiciones.get(destino.idTramo());
            saldos.set(indice, saldos.get(indice).add(operacion.saldoCapital()));
            cantidades[indice]++;
        }
        List<BigDecimal> porcentajes = CalculosCartera.participaciones(saldos);
        List<ResultadoRollRate.Transicion> transiciones = new ArrayList<>();
        for (int i = 0; i <= destinos.size(); i++) {
            Integer idTramo = i == cancelada ? null : destinos.get(i).idTramo();
            String nombre = i == cancelada ? "CANCELADA" : "TRAMO_" + destinos.get(i).numeroTramo();
            transiciones.add(new ResultadoRollRate.Transicion(idTramo, nombre, cantidades[i],
                    saldos.get(i), porcentajes.get(i)));
        }
        return new ResultadoRollRate.Fila(origen, operaciones.size(), CalculosCartera.saldo(operaciones), transiciones);
    }
}
