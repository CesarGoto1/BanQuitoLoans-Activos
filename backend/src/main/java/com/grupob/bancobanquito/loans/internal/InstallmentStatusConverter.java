package com.grupob.bancobanquito.loans.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class InstallmentStatusConverter implements AttributeConverter<InstallmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(InstallmentStatus status) {
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
    public InstallmentStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "PENDIENTE" -> InstallmentStatus.PENDING;
            case "PAGO_PARCIAL" -> InstallmentStatus.PARTIALLY_PAID;
            case "PAGADA" -> InstallmentStatus.PAID;
            default -> throw new IllegalArgumentException("Unknown installment status: " + value);
        };
    }
}
