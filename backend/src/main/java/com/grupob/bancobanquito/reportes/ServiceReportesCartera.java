package com.grupob.bancobanquito.reportes;

import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;

public interface ServiceReportesCartera {

    ResultadoMorosidad calcularMorosidad(CorteCartera corte);

    ResultadoComposicionCartera calcularComposicion(CorteCartera corte);

    ResultadoRollRate calcularRollRate(CorteCartera corteInicial, CorteCartera corteFinal);

    ResultadoConcentracion calcularConcentracion(CorteCartera corte, List<DatosOperacionReporte> datosOperaciones);
    ResultadoSaludCartera calcularSalud(CorteCartera corte, List<DatosOperacionReporte> datosOperaciones);
    ResultadoSaludCartera consultarSalud(LocalDate fechaCorte);

    ResultadoMorosidad consultarMorosidad(LocalDate fechaCorte);

    ResultadoComposicionCartera consultarComposicion(LocalDate fechaCorte);

    BigDecimal consultarCobertura(LocalDate fechaCorte);

    ResultadoRollRate consultarRollRate(LocalDate fechaInicial, LocalDate fechaFinal);
}
