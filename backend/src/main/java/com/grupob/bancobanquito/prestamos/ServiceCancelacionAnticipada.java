package com.grupob.bancobanquito.prestamos;

import java.math.BigDecimal;

public interface ServiceCancelacionAnticipada {

    LiquidacionCancelacion consultar(String numeroOperacion);

    LiquidacionCancelacion registrar(String numeroOperacion, BigDecimal montoRecibido, String cedulaAnalista);
}
