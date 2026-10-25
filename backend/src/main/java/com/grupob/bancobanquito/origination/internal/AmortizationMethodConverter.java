package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class AmortizationMethodConverter implements AttributeConverter<AmortizationMethod, String> {

    @Override
    public String convertToDatabaseColumn(AmortizationMethod method) {
        if (method == null) {
            return null;
        }
        return switch (method) {
            case FRENCH -> "FRANCES";
            case GERMAN -> "ALEMAN";
        };
    }

    @Override
    public AmortizationMethod convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "FRANCES" -> AmortizationMethod.FRENCH;
            case "ALEMAN" -> AmortizationMethod.GERMAN;
            default -> throw new IllegalArgumentException("Unknown amortization method: " + value);
        };
    }
}
