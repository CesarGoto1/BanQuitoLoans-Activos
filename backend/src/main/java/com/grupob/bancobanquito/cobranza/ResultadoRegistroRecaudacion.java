package com.grupob.bancobanquito.cobranza;

import com.grupob.bancobanquito.pagos.ResultadoPago;
import java.math.BigDecimal;
import java.util.UUID;

public record ResultadoRegistroRecaudacion(Integer idRegistro, int posicion, UUID uuidTransaccion,
                                           String numeroOperacion, BigDecimal montoRecibido,
                                           String estado, String codigoRechazo, ResultadoPago pago) {
}
