package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class ConversorMetodoAmortizacion implements AttributeConverter<MetodoAmortizacion, String> {

    @Override
    public String convertToDatabaseColumn(MetodoAmortizacion metodo) {
        if (metodo == null) {
            return null;
        }
        return switch (metodo) {
            case FRENCH -> "FRANCES";
            case GERMAN -> "ALEMAN";
        };
    }

    @Override
    public MetodoAmortizacion convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "FRANCES" -> MetodoAmortizacion.FRENCH;
            case "ALEMAN" -> MetodoAmortizacion.GERMAN;
            default -> throw new IllegalArgumentException("Unknown amortization method: " + value);
        };
    }
}
