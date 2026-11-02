package com.grupob.bancobanquito.reportes;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoSaludCartera(ResultadoMorosidad morosidad, ResultadoConcentracion concentracion,
        BigDecimal provisionesCarteraBruta, BigDecimal coberturaProvisiones,
        List<Categoria> porMontoOriginal, List<Categoria> porPlazo, List<Categoria> porSistemaAmortizacion) {

    public ResultadoSaludCartera {
        porMontoOriginal = List.copyOf(porMontoOriginal);
        porPlazo = List.copyOf(porPlazo);
        porSistemaAmortizacion = List.copyOf(porSistemaAmortizacion);
    }

    public record Categoria(String categoria, long cantidadOperaciones, BigDecimal saldoCapital,
                            BigDecimal participacion) {
    }
}
