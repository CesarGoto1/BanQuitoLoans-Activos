package com.grupob.bancobanquito.reportes.internal;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.reportes.CorteCartera;
import com.grupob.bancobanquito.reportes.DatosOperacionReporte;
import com.grupob.bancobanquito.reportes.ResultadoConcentracion;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CalculadoraConcentracion {

    ResultadoConcentracion calcular(CorteCartera corte, List<DatosOperacionReporte> datosOperaciones) {
        CalculosCartera.validar(corte);
        if (datosOperaciones == null) {
            throw new IllegalArgumentException("Los datos de las operaciones son obligatorios.");
        }
        Map<Integer, DatosOperacionReporte> datos = new HashMap<>();
        for (DatosOperacionReporte operacion : datosOperaciones) {
            if (operacion == null || operacion.idOperacion() == null || operacion.idOperacion() <= 0
                    || operacion.idCliente() == null || operacion.idCliente() <= 0
                    || datos.put(operacion.idOperacion(), operacion) != null) {
                throw new IllegalArgumentException("Los datos de clientes contienen identificadores inválidos o duplicados.");
            }
        }
        Map<Integer, List<CorteCartera.Operacion>> porCliente = new HashMap<>();
        List<CorteCartera.Operacion> bruta = corte.operaciones().stream()
                .filter(operacion -> operacion.estadoCartera() != EstadoCartera.CASTIGADA).toList();
        for (CorteCartera.Operacion operacion : bruta) {
            DatosOperacionReporte dato = datos.get(operacion.idOperacion());
            if (dato == null) {
                throw new IllegalArgumentException("Faltan datos del cliente para la operación " + operacion.idOperacion() + ".");
            }
            porCliente.computeIfAbsent(dato.idCliente(), id -> new ArrayList<>()).add(operacion);
        }
        BigDecimal saldoBruto = CalculosCartera.saldo(bruta);
        List<ResultadoConcentracion.Cliente> clientes = porCliente.entrySet().stream().map(entrada -> {
            BigDecimal saldo = CalculosCartera.saldo(entrada.getValue());
            return new ResultadoConcentracion.Cliente(entrada.getKey(), entrada.getValue().size(), saldo,
                    CalculosCartera.porcentaje(saldo, saldoBruto));
        }).sorted(Comparator.comparing(ResultadoConcentracion.Cliente::saldoCapital).reversed()
                .thenComparing(ResultadoConcentracion.Cliente::idCliente)).limit(10).toList();
        BigDecimal saldoMayores = clientes.stream().map(ResultadoConcentracion.Cliente::saldoCapital)
                .reduce(CalculosCartera.CERO, BigDecimal::add);
        return new ResultadoConcentracion(corte.fechaCorte(), saldoBruto, porCliente.size(), clientes, saldoMayores,
                CalculosCartera.porcentaje(saldoMayores, saldoBruto));
    }
}
