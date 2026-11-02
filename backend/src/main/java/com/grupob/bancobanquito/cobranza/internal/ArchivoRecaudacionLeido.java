package com.grupob.bancobanquito.cobranza.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

record ArchivoRecaudacionLeido(String nombre, String hashSha256, LocalDate fechaProceso,
                               LocalDateTime fechaGeneracion, BigDecimal montoTotal,
                               List<RegistroRecaudacionLeido> registros) {

    ArchivoRecaudacionLeido {
        registros = List.copyOf(registros);
    }
}
