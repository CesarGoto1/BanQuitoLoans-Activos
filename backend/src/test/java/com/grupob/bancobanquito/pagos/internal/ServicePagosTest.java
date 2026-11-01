package com.grupob.bancobanquito.pagos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.pagos.EstadoAplicacionPago;
import com.grupob.bancobanquito.pagos.PagoRecaudacion;
import com.grupob.bancobanquito.pagos.ResultadoPago;
import com.grupob.bancobanquito.pagos.ServicePagos;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Sql("/test-data-pagos.sql")
class ServicePagosTest {

        private static final LocalDate FECHA_NEGOCIO = LocalDate.of(2026, 12, 10);
        private static final String OPERACION_VENCIDA = "OP-20261031-009101";
        private static final String CEDULA_VENCIDA = "1710034065";

        @Autowired
        ServicePagos servicioPagos;

        @Autowired
        DetallePagoRepository detallePagoRepository;

        @Autowired
        PagoRecaudacionRepository pagoRecaudacionRepository;

        @Autowired
        JdbcTemplate jdbc;

        @Autowired
        EntityManager entityManager;

        @Test
        void pagoCompletoDejaLaCuotaPagadaYBajaElSaldo() {
                ResultadoPago resultado = servicioPagos.aplicarPagoRecaudacion(
                        pago(9101, OPERACION_VENCIDA, CEDULA_VENCIDA, "914.77"));

                assertThat(resultado.estado()).isEqualTo(EstadoAplicacionPago.APLICADO);
                assertThat(resultado.codigoRechazo()).isNull();
                assertThat(resultado.aplicadoSeguro()).isEqualByComparingTo("6.00");
                assertThat(resultado.aplicadoMora()).isEqualByComparingTo("3.33");
                assertThat(resultado.aplicadoInteres()).isEqualByComparingTo("130.03");
                assertThat(resultado.aplicadoCapital()).isEqualByComparingTo("775.41");

                Map<String, Object> cuota = cuota(9101, 1);
                assertThat(cuota.get("estadoCuota")).isEqualTo("PAGADA");
                assertThat(cuota.get("fechaPago").toString()).isEqualTo("2026-12-10");
                assertThat(saldo(9101)).isEqualByComparingTo("9224.59");
        }

        @Test
        void pagoParcialDejaLaCuotaEnPagoParcial() {
                servicioPagos.aplicarPagoRecaudacion(pago(9102, OPERACION_VENCIDA, CEDULA_VENCIDA, "500.00"));

                Map<String, Object> cuota = cuota(9101, 1);
                assertThat(cuota.get("estadoCuota")).isEqualTo("PAGO_PARCIAL");
                assertThat((BigDecimal) cuota.get("capitalPagado")).isEqualByComparingTo("360.64");
                assertThat(cuota.get("fechaPago")).isNull();
                assertThat(saldo(9101)).isEqualByComparingTo("9639.36");
        }

        @Test
        void guardaElPagoConSuDetalleYSusAsientos() {
                servicioPagos.aplicarPagoRecaudacion(pago(9103, OPERACION_VENCIDA, CEDULA_VENCIDA, "1000.00"));

                Pago pago = pagoRecaudacionRepository.findByIdRegistro(9103).orElseThrow();
                assertThat(pago.getOrigenPago()).isEqualTo(OrigenPago.ARCHIVO_RECAUDACION);
                assertThat(pago.getMontoRecibido()).isNull();
                assertThat(detallePagoRepository.findByIdPagoOrderByIdCuotaAsc(pago.getId())).hasSize(2);

                entityManager.flush();
                List<Map<String, Object>> asientos = jdbc.queryForList("""
                        SELECT a."claveIdempotencia", a."montoAsiento", d."codigoCuenta" AS debito, c."codigoCuenta" AS credito
                        FROM "asientosContables" a
                        JOIN "cuentasContables" d ON d."idCuentaContable" = a."idCuentaDebito"
                        JOIN "cuentasContables" c ON c."idCuentaContable" = a."idCuentaCredito"
                        WHERE a."idOperacion" = 9101 ORDER BY a."idAsiento"
                        """);
                assertThat(asientos).extracting(fila -> fila.get("credito")).containsExactly(
                        "SEGUROS_POR_PAGAR", "INTERESES_MORA_POR_COBRAR", "INTERESES_POR_COBRAR", "CARTERA_VENCIDA");
                assertThat(asientos).allMatch(fila -> fila.get("debito").equals("RECAUDACIONES_POR_APLICAR"));
                assertThat(asientos.stream().map(fila -> (BigDecimal) fila.get("montoAsiento"))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("1000.00");
        }

        @Test
        void rechazaLaOperacionInexistente() {
                assertThat(servicioPagos.aplicarPagoRecaudacion(pago(9104, "OP-00000000-000000", CEDULA_VENCIDA, "10.00"))
                        .codigoRechazo()).isEqualTo("REG-003");
        }

        @Test
        void rechazaLaIdentificacionQueNoEsDelTitular() {
                ResultadoPago resultado = servicioPagos.aplicarPagoRecaudacion(
                        pago(9105, OPERACION_VENCIDA, "0912345675", "10.00"));

                assertThat(resultado.estado()).isEqualTo(EstadoAplicacionPago.RECHAZADO);
                assertThat(resultado.codigoRechazo()).isEqualTo("REG-004");
                assertThat(saldo(9101)).isEqualByComparingTo("10000.00");
        }

        @Test
        void rechazaLaOperacionCancelada() {
                assertThat(servicioPagos.aplicarPagoRecaudacion(
                        pago(9106, "OP-20260430-009103", "1102233445", "10.00")).codigoRechazo()).isEqualTo("REG-006");
        }

        @Test
        void elSobrepagoCancelaLaOperacionYDevuelveElExcedente() {
                ResultadoPago resultado = servicioPagos.aplicarPagoRecaudacion(
                        pago(9107, "OP-20261031-009104", "1102233446", "250.00"));

                assertThat(resultado.estado()).isEqualTo(EstadoAplicacionPago.APLICADO);
                assertThat(resultado.codigoRechazo()).isEqualTo("REG-007");
                assertThat(resultado.excedenteDevolver()).isEqualByComparingTo("41.00");
                assertThat(saldo(9104)).isEqualByComparingTo("0");
                assertThat(jdbc.queryForObject("SELECT \"estadoCartera\" FROM operaciones WHERE \"idOperacion\" = 9104",
                        String.class)).isEqualTo("CANCELADA");
                assertThat(pagoRecaudacionRepository.findByIdRegistro(9107).orElseThrow().getExcedenteDevuelto())
                        .isEqualByComparingTo("41.00");
        }

        @Test
        void elPagoDeUnaOperacionCastigadaSeRegistraComoRecuperacion() {
                ResultadoPago resultado = servicioPagos.aplicarPagoRecaudacion(
                        pago(9108, "OP-20260430-009102", "0912345675", "100.00"));

                assertThat(resultado.estado()).isEqualTo(EstadoAplicacionPago.APLICADO);
                entityManager.flush();
                assertThat(jdbc.queryForList("""
                        SELECT a."tipoAsiento", c."codigoCuenta", a."montoAsiento"
                        FROM "asientosContables" a JOIN "cuentasContables" c ON c."idCuentaContable" = a."idCuentaCredito"
                        WHERE a."idOperacion" = 9102
                        """)).singleElement().satisfies(fila -> {
                        assertThat(fila.get("tipoAsiento")).isEqualTo("RECUPERACION");
                        assertThat(fila.get("codigoCuenta")).isEqualTo("INGRESOS_RECUPERACIONES");
                        assertThat((BigDecimal) fila.get("montoAsiento")).isEqualByComparingTo("100.00");
                        });
        }

        @Test
        void noAplicaDosVecesElMismoRegistro() {
                servicioPagos.aplicarPagoRecaudacion(pago(9109, OPERACION_VENCIDA, CEDULA_VENCIDA, "100.00"));

                assertThatThrownBy(() -> servicioPagos.aplicarPagoRecaudacion(
                        pago(9109, OPERACION_VENCIDA, CEDULA_VENCIDA, "100.00")))
                        .isInstanceOf(IllegalStateException.class);
                assertThat(saldo(9101)).isEqualByComparingTo("10000.00");
        }

        private PagoRecaudacion pago(int idRegistro, String numeroOperacion, String cedula, String monto) {
                return new PagoRecaudacion(idRegistro, UUID.randomUUID(), numeroOperacion, cedula, new BigDecimal(monto),
                        LocalDateTime.of(2026, 12, 10, 9, 0), "VEN", FECHA_NEGOCIO);
        }

        private Map<String, Object> cuota(int idOperacion, int numeroCuota) {
                entityManager.flush();
                return jdbc.queryForMap("SELECT * FROM cuotas WHERE \"idOperacion\" = ? AND \"numeroCuota\" = ?",
                        idOperacion, numeroCuota);
        }

        private BigDecimal saldo(int idOperacion) {
                entityManager.flush();
                return jdbc.queryForObject("SELECT \"saldoCapital\" FROM operaciones WHERE \"idOperacion\" = ?",
                        BigDecimal.class, idOperacion);
        }
}
