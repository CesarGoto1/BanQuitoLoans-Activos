package com.grupob.bancobanquito.prestamos.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class ConversorEstadoCuota implements AttributeConverter<EstadoCuota, String> {

    @Override
    public String convertToDatabaseColumn(EstadoCuota status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "PENDIENTE";
            case PARTIALLY_PAID -> "PAGO_PARCIAL";
            case PAID -> "PAGADA";
        };
    }

    @Override
    public EstadoCuota convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "PENDIENTE" -> EstadoCuota.PENDING;
            case "PAGO_PARCIAL" -> EstadoCuota.PARTIALLY_PAID;
            case "PAGADA" -> EstadoCuota.PAID;
            default -> throw new IllegalArgumentException("Unknown installment status: " + value);
        };
    }
}
