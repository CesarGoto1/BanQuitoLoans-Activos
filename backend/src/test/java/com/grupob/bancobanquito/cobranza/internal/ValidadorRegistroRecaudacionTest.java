package com.grupob.bancobanquito.cobranza.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidadorRegistroRecaudacionTest {

    private static final String DETALLE = "D,1,3f6c2a1e-8b4d-4c7a-9e2f-1a2b3c4d5e01,OP-20261031-000001,1710034065,10.00,2026-10-31T09:15:00,VEN,";
    private final ValidadorRegistroRecaudacion validador = new ValidadorRegistroRecaudacion();

    @ParameterizedTest
    @ValueSource(strings = {"VEN", "WEB", "DEB", "TRF"})
    void admiteLosCanalesDefinidos(String canal) {
        var registro = validador.validar(DETALLE.replace(",VEN,", "," + canal + ","), 1);
        assertThat(registro.codigoRechazo()).isNull();
        assertThat(registro.referencia()).isEmpty();
        assertThat(registro.monto()).isEqualByComparingTo("10.00");
    }

    @ParameterizedTest
    @CsvSource({"0.00,REG-005", "-1.00,REG-005", "1.001,REG-005", "1.000,REG-005", "1e2,REG-001", "NaN,REG-001", "1.0,REG-001", "100000000.00,REG-001"})
    void validaMontosSinRedondear(String monto, String codigo) {
        assertThat(validador.validar(DETALLE.replace(",10.00,", "," + monto + ","), 1).codigoRechazo()).isEqualTo(codigo);
    }

    @ParameterizedTest
    @CsvSource({"1,0", "1,2", "1,999999999999999999", "2,1-1-1-1-1", "3,''", "4,171003406", "6,2026-02-30T09:15:00", "6,2026-10-31T09:15", "7,CAJA"})
    void rechazaCamposInvalidos(int indice, String valor) {
        String[] campos = DETALLE.split(",", -1);
        campos[indice] = valor;
        assertThat(validador.validar(String.join(",", campos), 1).codigoRechazo()).isEqualTo("REG-001");
    }

    @Test
    void admiteComasYComillasEscapadasEnLaReferencia() {
        var registro = validador.validar(DETALLE + "\"Cuota, cliente \"\"prueba\"\"\"", 1);
        assertThat(registro.codigoRechazo()).isNull();
        assertThat(registro.referencia()).isEqualTo("Cuota, cliente \"prueba\"");
    }

    @Test
    void rechazaUnaReferenciaQueExcedeLaLongitudDeLaTabla() {
        assertThat(validador.validar(DETALLE + "a".repeat(101), 1).codigoRechazo()).isEqualTo("REG-001");
    }

    @Test
    void rechazaCamposAusentesOSobrantes() {
        assertThat(validador.validar(DETALLE.substring(0, DETALLE.length() - 1), 1).codigoRechazo()).isEqualTo("REG-001");
        assertThat(validador.validar(DETALLE + ",extra", 1).codigoRechazo()).isEqualTo("REG-001");
    }
}
