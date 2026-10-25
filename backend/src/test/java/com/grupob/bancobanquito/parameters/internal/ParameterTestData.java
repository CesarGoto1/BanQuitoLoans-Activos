package com.grupob.bancobanquito.parameters.internal;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;

final class ParameterTestData {
    static final LocalDate DATE = LocalDate.of(2026, 10, 31);

    static void load(EntityManager entities, ParameterRepository repository) {
        repository.deleteAll();
        Map<String, String> values = Map.of("PAR-01", "500", "PAR-02", "30000", "PAR-03", "6", "PAR-04", "60",
                "PAR-05", "0.1677", "PAR-06", "0.1550", "PAR-07", "0.0006", "PAR-08", "0.0050");
        values.forEach((code, value) -> save(entities, code, value, DATE, null));
        if (entities.find(BusinessDay.class, 1) == null) {
            var day = new BusinessDay(1);
            ReflectionTestUtils.setField(day, "businessDate", DATE);
            ReflectionTestUtils.setField(day, "status", ClosingStatus.PENDING);
            entities.persist(day);
        }
    }

    static void save(EntityManager entities, String code, String value, LocalDate from, LocalDate to) {
        var parameter = new Parameter();
        parameter.setCode(code);
        parameter.setName(code);
        parameter.setValue(new BigDecimal(value));
        parameter.setValidFrom(from);
        parameter.setValidTo(to);
        parameter.setCreatedBy("pruebas");
        parameter.setCreatedAt(DATE.atStartOfDay());
        entities.persist(parameter);
    }
}
