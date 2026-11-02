package com.grupob.bancobanquito.cobranza.internal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

record RegistroRecaudacionLeido(int posicion, String lineaOriginal, UUID uuidTransaccion,
                                String numeroOperacion, String identificacion, BigDecimal monto,
                                LocalDateTime fechaCobro, String canal, String referencia,
                                String codigoRechazo) {
}
