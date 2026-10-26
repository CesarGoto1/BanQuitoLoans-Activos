package com.grupob.bancobanquito.parametros.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class ConversorEstadoCierre implements AttributeConverter<EstadoCierre, String> {

    @Override
    public String convertToDatabaseColumn(EstadoCierre status) {
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
    public EstadoCierre convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "PENDIENTE" -> EstadoCierre.PENDING;
            case "EN_PROCESO" -> EstadoCierre.IN_PROGRESS;
            case "COMPLETADO" -> EstadoCierre.COMPLETED;
            case "FALLIDO" -> EstadoCierre.FAILED;
            default -> throw new IllegalArgumentException("Unknown closing status: " + value);
        };
    }
}
