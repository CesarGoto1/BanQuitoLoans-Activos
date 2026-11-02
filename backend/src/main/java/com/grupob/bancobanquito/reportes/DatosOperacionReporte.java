package com.grupob.bancobanquito.reportes;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.math.BigDecimal;

public record DatosOperacionReporte(Integer idOperacion, Integer idCliente, BigDecimal montoOriginal,
                                    Integer plazoMeses, MetodoAmortizacion sistemaAmortizacion) {
}
