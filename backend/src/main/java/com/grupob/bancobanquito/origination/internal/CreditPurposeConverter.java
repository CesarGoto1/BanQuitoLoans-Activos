package com.grupob.bancobanquito.origination.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class CreditPurposeConverter implements AttributeConverter<CreditPurpose, String> {

    @Override
    public String convertToDatabaseColumn(CreditPurpose purpose) {
        if (purpose == null) {
            return null;
        }
        return switch (purpose) {
            case DURABLE_GOODS -> "BIENES_DURADEROS";
            case HEALTH -> "SALUD";
            case EDUCATION -> "EDUCACION";
            case TRAVEL -> "VIAJE";
            case OTHER -> "OTRO";
        };
    }

    @Override
    public CreditPurpose convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "BIENES_DURADEROS" -> CreditPurpose.DURABLE_GOODS;
            case "SALUD" -> CreditPurpose.HEALTH;
            case "EDUCACION" -> CreditPurpose.EDUCATION;
            case "VIAJE" -> CreditPurpose.TRAVEL;
            case "OTRO" -> CreditPurpose.OTHER;
            default -> throw new IllegalArgumentException("Unknown credit purpose: " + value);
        };
    }
}
