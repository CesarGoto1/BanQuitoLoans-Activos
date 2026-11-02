package com.grupob.bancobanquito.reportes.internal;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.reportes.CorteCartera;
import com.grupob.bancobanquito.reportes.DatosOperacionReporte;
import com.grupob.bancobanquito.reportes.ResultadoMorosidad;
import com.grupob.bancobanquito.reportes.ResultadoConcentracion;
import com.grupob.bancobanquito.reportes.ResultadoSaludCartera;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class CalculadoraSaludCartera {

    ResultadoSaludCartera calcular(CorteCartera corte, List<DatosOperacionReporte> datos,
                                   ResultadoMorosidad morosidad, ResultadoConcentracion concentracion) {
        List<CorteCartera.Operacion> bruta = corte.operaciones().stream()
                .filter(operacion -> operacion.estadoCartera() != EstadoCartera.CASTIGADA).toList();
        Map<Integer, DatosOperacionReporte> porId = datos.stream()
                .collect(Collectors.toMap(DatosOperacionReporte::idOperacion, dato -> dato));
        for (CorteCartera.Operacion operacion : bruta) {
            DatosOperacionReporte dato = porId.get(operacion.idOperacion());
            CalculosCartera.validarImporte(dato.montoOriginal());
            if (dato.montoOriginal().signum() == 0 || dato.montoOriginal().compareTo(new BigDecimal("30000")) > 0
                    || dato.plazoMeses() == null || dato.plazoMeses() < 1 || dato.plazoMeses() > 60
                    || dato.sistemaAmortizacion() == null) {
                throw new IllegalArgumentException("Los datos de la operación no corresponden a las categorías de RF-23.");
            }
        }
        BigDecimal provisiones = CalculosCartera.provision(bruta);
        return new ResultadoSaludCartera(morosidad, concentracion, provisiones,
                CalculosCartera.porcentaje(provisiones, morosidad.saldoVencido().add(morosidad.saldoEnLitigio())),
                distribuir(bruta, porId, List.of("Hasta USD 2.000,00", "Mayor a USD 2.000,00 y hasta USD 10.000,00",
                        "Mayor a USD 10.000,00 y hasta USD 30.000,00"), dato ->
                        dato.montoOriginal().compareTo(new BigDecimal("2000")) <= 0 ? 0
                                : dato.montoOriginal().compareTo(new BigDecimal("10000")) <= 0 ? 1 : 2),
                distribuir(bruta, porId, List.of("Hasta 12 meses", "De 13 a 36 meses", "De 37 a 60 meses"),
                        dato -> dato.plazoMeses() <= 12 ? 0 : dato.plazoMeses() <= 36 ? 1 : 2),
                distribuir(bruta, porId, List.of("Francés", "Alemán"), dato -> switch (dato.sistemaAmortizacion()) {
                    case FRANCES -> 0;
                    case ALEMAN -> 1;
                }));
    }

    private List<ResultadoSaludCartera.Categoria> distribuir(List<CorteCartera.Operacion> operaciones,
            Map<Integer, DatosOperacionReporte> datos, List<String> nombres,
            Function<DatosOperacionReporte, Integer> clasificar) {
        List<BigDecimal> saldos = new ArrayList<>();
        long[] cantidades = new long[nombres.size()];
        nombres.forEach(nombre -> saldos.add(CalculosCartera.CERO));
        for (CorteCartera.Operacion operacion : operaciones) {
            int indice = clasificar.apply(datos.get(operacion.idOperacion()));
            cantidades[indice]++;
            saldos.set(indice, saldos.get(indice).add(operacion.saldoCapital()));
        }
        BigDecimal total = CalculosCartera.saldo(operaciones);
        List<ResultadoSaludCartera.Categoria> resultado = new ArrayList<>();
        for (int i = 0; i < nombres.size(); i++) {
            resultado.add(new ResultadoSaludCartera.Categoria(nombres.get(i), cantidades[i], saldos.get(i),
                    CalculosCartera.porcentaje(saldos.get(i), total)));
        }
        return resultado;
    }
}
