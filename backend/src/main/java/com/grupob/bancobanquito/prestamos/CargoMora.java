package com.grupob.bancobanquito.prestamos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CargoMora(
        Integer numeroCuota,
        LocalDate fechaVencimiento,
        String estadoCuota,
        BigDecimal moraGenerada,
        BigDecimal moraPagada,
        BigDecimal moraPendiente) {
}
