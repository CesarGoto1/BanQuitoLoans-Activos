package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cobranza.ResultadoRecaudacion;
import com.grupob.bancobanquito.cobranza.ResultadoRegistroRecaudacion;
import com.grupob.bancobanquito.pagos.EstadoAplicacionPago;
import com.grupob.bancobanquito.pagos.ResultadoPago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EscritorNovedadesRecaudacionTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);
    private final EscritorNovedadesRecaudacion escritor = new EscritorNovedadesRecaudacion();

    @Test
    void archivoRechazadoSoloTieneCabecera() {
        ResultadoRecaudacion resultado = new ResultadoRecaudacion(1, "REC_20261031_001.csv", "RECHAZADO", "ARC-005", List.of());

        assertThat(escritor.contenido(resultado, FECHA))
                .isEqualTo("H,REC_20261031_001,2026-10-31,RECHAZADO,ARC-005\n");
    }

    @Test
    void archivoProcesadoDetallaImputacionYCuadraMontos() {
        UUID primero = UUID.fromString("3f6c2a1e-8b4d-4c7a-9e2f-1a2b3c4d5e01");
        UUID segundo = UUID.fromString("7a9d4e2b-1c3f-4b6a-8d5e-2b3c4d5e6f02");
        UUID tercero = UUID.fromString("9b1e5f3c-2d4a-4c7b-9e6f-3c4d5e6f7a03");
        ResultadoPago sobrepago = new ResultadoPago(EstadoAplicacionPago.APLICADO, "REG-007",
                new BigDecimal("6.00"), new BigDecimal("3.33"), new BigDecimal("50.00"),
                new BigDecimal("140.67"), BigDecimal.ZERO, new BigDecimal("10.00"));
        ResultadoRecaudacion resultado = new ResultadoRecaudacion(1, "REC_20261031_001.csv", "PROCESADO", null, List.of(
                new ResultadoRegistroRecaudacion(3, 3, tercero, "PRE-0003", new BigDecimal("50.00"), "DUPLICADO", "REG-002", null),
                new ResultadoRegistroRecaudacion(1, 1, primero, "PRE-0001", new BigDecimal("210.00"), "APLICADO", "REG-007", sobrepago),
                new ResultadoRegistroRecaudacion(2, 2, segundo, "PRE-9999", new BigDecimal("40.00"), "RECHAZADO", "REG-003", null)));

        String[] lineas = escritor.contenido(resultado, FECHA).split("\n");

        assertThat(lineas).containsExactly(
                "H,REC_20261031_001,2026-10-31,PROCESADO,",
                "D,1," + primero + ",PRE-0001,APLICADO,REG-007,Sobrepago: el excedente se devuelve,210.00,6.00,3.33,50.00,140.67,0.00,10.00",
                "D,2," + segundo + ",PRE-9999,RECHAZADO,REG-003,La operacion no existe,40.00,0.00,0.00,0.00,0.00,0.00,40.00",
                "D,3," + tercero + ",PRE-0003,DUPLICADO,REG-002,Transaccion duplicada,50.00,0.00,0.00,0.00,0.00,0.00,0.00",
                "T,3,1,1,1,200.00,100.00");
    }
}
