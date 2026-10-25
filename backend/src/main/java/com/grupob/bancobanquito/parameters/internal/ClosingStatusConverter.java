package com.grupob.bancobanquito.parameters.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class ClosingStatusConverter implements AttributeConverter<ClosingStatus, String> {

    @Override
    public String convertToDatabaseColumn(ClosingStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "PENDIENTE";
            case IN_PROGRESS -> "EN_PROCESO";
            case COMPLETED -> "COMPLETADO";
            case FAILED -> "FALLIDO";
        };
    }

    @Override
    public ClosingStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "PENDIENTE" -> ClosingStatus.PENDING;
            case "EN_PROCESO" -> ClosingStatus.IN_PROGRESS;
            case "COMPLETADO" -> ClosingStatus.COMPLETED;
            case "FALLIDO" -> ClosingStatus.FAILED;
            default -> throw new IllegalArgumentException("Unknown closing status: " + value);
        };
    }
}
