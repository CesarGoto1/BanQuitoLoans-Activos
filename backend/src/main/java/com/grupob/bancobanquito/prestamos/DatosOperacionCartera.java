package com.grupob.bancobanquito.prestamos;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.math.BigDecimal;

public record DatosOperacionCartera(Integer idOperacion, Integer idCliente, BigDecimal montoAprobado,
                                    Integer plazoMeses, MetodoAmortizacion sistemaAmortizacion) {
}
