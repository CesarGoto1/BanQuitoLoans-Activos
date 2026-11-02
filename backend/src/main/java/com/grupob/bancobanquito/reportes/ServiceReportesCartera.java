package com.grupob.bancobanquito.reportes;

import java.util.List;

public interface ServiceReportesCartera {

    ResultadoMorosidad calcularMorosidad(CorteCartera corte);

    ResultadoComposicionCartera calcularComposicion(CorteCartera corte);

    ResultadoRollRate calcularRollRate(CorteCartera corteInicial, CorteCartera corteFinal);

    ResultadoConcentracion calcularConcentracion(CorteCartera corte, List<DatosOperacionReporte> datosOperaciones);
    ResultadoSaludCartera calcularSalud(CorteCartera corte, List<DatosOperacionReporte> datosOperaciones);
}

