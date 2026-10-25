package com.grupob.bancobanquito.parametrizacion;

import java.math.BigDecimal;

public interface ParametroService {

    /**
     * Consulta el valor numérico exacto de un parámetro de negocio por su código (ej. PAR-09).
     *
     * @param codigoParametro Código formal del parámetro (ej. "PAR-09").
     * @return Valor del parámetro en BigDecimal.
     */
    BigDecimal valor(String codigoParametro);
}
