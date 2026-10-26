package com.grupob.bancobanquito.prestamos.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class ConversorEstadoInstruccionDesembolso implements AttributeConverter<EstadoInstruccionDesembolso, String> {

    @Override
    public String convertToDatabaseColumn(EstadoInstruccionDesembolso status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "PENDIENTE";
            case DELIVERED -> "ENTREGADA";
        };
    }

    @Override
    public EstadoInstruccionDesembolso convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "PENDIENTE" -> EstadoInstruccionDesembolso.PENDING;
            case "ENTREGADA" -> EstadoInstruccionDesembolso.DELIVERED;
            default -> throw new IllegalArgumentException("Unknown disbursement instruction status: " + value);
        };
    }
}
