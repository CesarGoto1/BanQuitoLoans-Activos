package com.grupob.bancobanquito.originacion.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class ConversorDestinoCredito implements AttributeConverter<DestinoCredito, String> {

    @Override
    public String convertToDatabaseColumn(DestinoCredito destinoCredito) {
        if (destinoCredito == null) {
            return null;
        }
        return switch (destinoCredito) {
            case DURABLE_GOODS -> "BIENES_DURADEROS";
            case HEALTH -> "SALUD";
            case EDUCATION -> "EDUCACION";
            case TRAVEL -> "VIAJE";
            case OTHER -> "OTRO";
        };
    }

    @Override
    public DestinoCredito convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "BIENES_DURADEROS" -> DestinoCredito.DURABLE_GOODS;
            case "SALUD" -> DestinoCredito.HEALTH;
            case "EDUCACION" -> DestinoCredito.EDUCATION;
            case "VIAJE" -> DestinoCredito.TRAVEL;
            case "OTRO" -> DestinoCredito.OTHER;
            default -> throw new IllegalArgumentException("Unknown credit purpose: " + value);
        };
    }
}
