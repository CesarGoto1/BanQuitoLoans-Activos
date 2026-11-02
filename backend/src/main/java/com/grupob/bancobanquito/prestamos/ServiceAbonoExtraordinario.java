package com.grupob.bancobanquito.prestamos;

import com.grupob.bancobanquito.amortizacion.ModalidadAbono;
import java.math.BigDecimal;

public interface ServiceAbonoExtraordinario {

    ResultadoAbono simular(String numeroOperacion, BigDecimal monto, ModalidadAbono modalidad);

    ResultadoAbono registrar(String numeroOperacion, BigDecimal monto, ModalidadAbono modalidad,
                             String cedulaAnalista);
}
