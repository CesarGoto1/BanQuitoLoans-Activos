package com.grupob.bancobanquito.cobranza.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.cobranza.ResultadoRecaudacion;
import com.grupob.bancobanquito.cobranza.ServiceRecaudaciones;
import com.grupob.bancobanquito.pagos.EstadoAplicacionPago;
import com.grupob.bancobanquito.pagos.PagoRecaudacion;
import com.grupob.bancobanquito.pagos.ResultadoPago;
import com.grupob.bancobanquito.pagos.ServicePagos;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringJUnitConfig(PersistenciaRecaudacionesTest.Configuracion.class)
@EnabledIfEnvironmentVariable(named = "RECAUDACIONES_TEST_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:55439/banquito_recaudaciones_test")
class PersistenciaRecaudacionesTest {

    private static final String UUID_PRIMERO = "3f6c2a1e-8b4d-4c7a-9e2f-1a2b3c4d5e01";
    private static final String UUID_SEGUNDO = "7a9d4e2b-1c3f-4b6a-8d5e-2b3c4d5e6f02";

    @Autowired
    ServiceRecaudaciones recaudaciones;

    @Autowired
    ServicePagos pagos;

    @Autowired
    DataSource dataSource;

    @BeforeEach
    void preparar() throws Exception {
        try (Connection conexion = dataSource.getConnection(); var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("DELETE FROM test_payment_effect");
            sentencia.executeUpdate("DELETE FROM \"registrosRecaudacion\"");
            sentencia.executeUpdate("DELETE FROM \"archivosRecaudacion\"");
        }
        reset(pagos);
        when(pagos.aplicarPagoRecaudacion(any())).thenAnswer(invocacion -> {
            PagoRecaudacion pago = invocacion.getArgument(0);
            registrarEfecto(pago);
            return aplicado();
        });
    }

    @Test
    void confirmaArchivoRegistroYEfectoEnLaMismaTransaccion() throws Exception {
        var resultado = procesar("001", List.of(detalle(1, UUID_PRIMERO)));
        assertThat(resultado.estado()).isEqualTo("PROCESADO");
        assertThat(resultado.registros()).hasSize(1);
        assertThat(resultado.registros().getFirst().estado()).isEqualTo("APLICADO");
        assertThat(contar("SELECT count(*) FROM test_payment_effect")).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM \"registrosRecaudacion\" WHERE \"estadoRegistro\" = 'APLICADO'")).isEqualTo(1);
    }

    @Test
    void rechazaElArchivoRepetidoSinVolverACobrar() throws Exception {
        procesar("001", List.of(detalle(1, UUID_PRIMERO)));
        var repetido = procesar("001", List.of(detalle(1, UUID_PRIMERO)));
        assertThat(repetido.codigoRechazo()).isEqualTo("ARC-006");
        assertThat(repetido.registros()).isEmpty();
        verify(pagos, times(1)).aplicarPagoRecaudacion(any());
        assertThat(contar("SELECT count(*) FROM \"archivosRecaudacion\"")).isEqualTo(2);
    }

    @Test
    void detectaUuidAplicadoEnOtroArchivo() throws Exception {
        procesar("001", List.of(detalle(1, UUID_PRIMERO)));
        var resultado = procesar("002", List.of(detalle(1, UUID_PRIMERO)));
        assertThat(resultado.registros().getFirst().codigoRechazo()).isEqualTo("REG-002");
        assertThat(resultado.registros().getFirst().estado()).isEqualTo("DUPLICADO");
        verify(pagos, times(1)).aplicarPagoRecaudacion(any());
    }

    @Test
    void detectaUuidRepetidoDentroDelArchivo() throws Exception {
        var resultado = procesar("001", List.of(detalle(1, UUID_PRIMERO), detalle(2, UUID_PRIMERO)));
        assertThat(resultado.registros().getLast().codigoRechazo()).isEqualTo("REG-002");
        verify(pagos, times(1)).aplicarPagoRecaudacion(any());
    }

    @Test
    void conservaElRechazoDeNegocioYContinua() throws Exception {
        reset(pagos);
        when(pagos.aplicarPagoRecaudacion(any())).thenAnswer(invocacion -> {
            PagoRecaudacion pago = invocacion.getArgument(0);
            if (pago.uuidTransaccion().equals(UUID.fromString(UUID_PRIMERO))) {
                return new ResultadoPago(EstadoAplicacionPago.RECHAZADO, "REG-003",
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }
            registrarEfecto(pago);
            return aplicado();
        });
        var resultado = procesar("001", List.of(detalle(1, UUID_PRIMERO), detalle(2, UUID_SEGUNDO)));
        assertThat(resultado.registros().getFirst().codigoRechazo()).isEqualTo("REG-003");
        assertThat(resultado.registros().getLast().estado()).isEqualTo("APLICADO");
        assertThat(contar("SELECT count(*) FROM test_payment_effect")).isEqualTo(1);
    }

    @Test
    void unUuidRechazadoPuedeCorregirseEnOtroArchivo() throws Exception {
        reset(pagos);
        when(pagos.aplicarPagoRecaudacion(any())).thenReturn(new ResultadoPago(EstadoAplicacionPago.RECHAZADO,
                "REG-003", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        procesar("001", List.of(detalle(1, UUID_PRIMERO)));
        reset(pagos);
        when(pagos.aplicarPagoRecaudacion(any())).thenReturn(aplicado());
        assertThat(procesar("002", List.of(detalle(1, UUID_PRIMERO))).registros())
                .allSatisfy(registro -> assertThat(registro.estado()).isEqualTo("APLICADO"));
    }

    @Test
    void unFalloTecnicoRevierteTodoYPermiteReintentar() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        reset(pagos);
        when(pagos.aplicarPagoRecaudacion(any())).thenAnswer(invocacion -> {
            registrarEfecto(invocacion.getArgument(0));
            if (llamadas.incrementAndGet() == 2) {
                throw new IllegalStateException("Fallo de prueba después de escribir.");
            }
            return aplicado();
        });
        List<String> detalles = List.of(detalle(1, UUID_PRIMERO), detalle(2, UUID_SEGUNDO));
        assertThatThrownBy(() -> procesar("001", detalles)).isInstanceOf(IllegalStateException.class);
        assertThat(contar("SELECT count(*) FROM test_payment_effect")).isZero();
        assertThat(contar("SELECT count(*) FROM \"archivosRecaudacion\"")).isZero();
        assertThat(contar("SELECT count(*) FROM \"registrosRecaudacion\"")).isZero();
        assertThat(procesar("001", detalles).estado()).isEqualTo("PROCESADO");
        assertThat(contar("SELECT count(*) FROM test_payment_effect")).isEqualTo(2);
    }

    @Test
    void unaRespuestaDePagosIncompatibleRevierteElIntento() throws Exception {
        reset(pagos);
        when(pagos.aplicarPagoRecaudacion(any())).thenAnswer(invocacion -> {
            registrarEfecto(invocacion.getArgument(0));
            return new ResultadoPago(EstadoAplicacionPago.RECHAZADO, "REG-007", null, null, null, null, null, null);
        });
        assertThatThrownBy(() -> procesar("001", List.of(detalle(1, UUID_PRIMERO)))).isInstanceOf(IllegalStateException.class);
        assertThat(contar("SELECT count(*) FROM test_payment_effect")).isZero();
        assertThat(contar("SELECT count(*) FROM \"archivosRecaudacion\"")).isZero();
    }

    @Test
    void persisteRechazosDeArchivoSinInvocarPagos() throws Exception {
        var resultado = recaudaciones.procesar("invalido.csv", new byte[0]);
        assertThat(resultado.codigoRechazo()).isEqualTo("ARC-001");
        assertThat(contar("SELECT count(*) FROM \"archivosRecaudacion\" WHERE \"codigoRechazo\" = 'ARC-001'")).isEqualTo(1);
        verifyNoInteractions(pagos);
    }

    @Test
    void persisteRechazosIndividualesSinInvocarPagos() throws Exception {
        var resultado = procesar("001", List.of(detalle(1, UUID_PRIMERO).replace(",VEN,", ",OTRO,")));
        assertThat(resultado.registros().getFirst().codigoRechazo()).isEqualTo("REG-001");
        assertThat(contar("SELECT count(*) FROM \"registrosRecaudacion\" WHERE \"canalPago\" IS NULL")).isEqualTo(1);
        verifyNoInteractions(pagos);
    }

    @Test
    void dosArchivosSimultaneosConElMismoUuidSoloCobranUnaVez() throws Exception {
        List<ResultadoRecaudacion> resultados = procesarConcurrentemente("001", "002");
        assertThat(resultados).allSatisfy(resultado -> assertThat(resultado.estado()).isEqualTo("PROCESADO"));
        assertThat(resultados.stream().flatMap(resultado -> resultado.registros().stream()).map(registro -> registro.estado()))
                .containsExactlyInAnyOrder("APLICADO", "DUPLICADO");
        assertThat(contar("SELECT count(*) FROM test_payment_effect")).isEqualTo(1);
        verify(pagos, times(1)).aplicarPagoRecaudacion(any());
    }

    @Test
    void dosRecepcionesSimultaneasDelMismoArchivoSoloCobranUnaVez() throws Exception {
        List<ResultadoRecaudacion> resultados = procesarConcurrentemente("001", "001");
        assertThat(resultados.stream().map(ResultadoRecaudacion::estado)).containsExactlyInAnyOrder("PROCESADO", "RECHAZADO");
        assertThat(resultados.stream().filter(resultado -> resultado.codigoRechazo() != null).map(ResultadoRecaudacion::codigoRechazo))
                .containsExactly("ARC-006");
        assertThat(contar("SELECT count(*) FROM test_payment_effect")).isEqualTo(1);
        verify(pagos, times(1)).aplicarPagoRecaudacion(any());
    }

    private List<ResultadoRecaudacion> procesarConcurrentemente(String primero, String segundo) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var tareaPrimera = ejecutor.submit(() -> {
                salida.await();
                return procesar(primero, List.of(detalle(1, UUID_PRIMERO)));
            });
            var tareaSegunda = ejecutor.submit(() -> {
                salida.await();
                return procesar(segundo, List.of(detalle(1, UUID_PRIMERO)));
            });
            salida.countDown();
            return List.of(tareaPrimera.get(20, TimeUnit.SECONDS), tareaSegunda.get(20, TimeUnit.SECONDS));
        }
    }

    private void registrarEfecto(PagoRecaudacion pago) throws Exception {
        Connection conexion = DataSourceUtils.getConnection(dataSource);
        try (var sentencia = conexion.prepareStatement("INSERT INTO test_payment_effect (id_registro) VALUES (?)")) {
            sentencia.setInt(1, pago.idRegistro());
            sentencia.executeUpdate();
        } finally {
            DataSourceUtils.releaseConnection(conexion, dataSource);
        }
    }

    private long contar(String consulta) throws Exception {
        try (Connection conexion = dataSource.getConnection(); var sentencia = conexion.createStatement();
             var resultado = sentencia.executeQuery(consulta)) {
            resultado.next();
            return resultado.getLong(1);
        }
    }

    private ResultadoPago aplicado() {
        return new ResultadoPago(EstadoAplicacionPago.APLICADO, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private String detalle(int posicion, String uuid) {
        return "D," + posicion + "," + uuid + ",OP-20261031-000001,1710034065,10.00,2026-10-31T09:15:00,VEN,";
    }

    private ResultadoRecaudacion procesar(String secuencia, List<String> detalles) throws Exception {
        String identificador = "REC_20261031_" + secuencia;
        String cuerpo = String.join("\n", detalles);
        String total = new BigDecimal("10.00").multiply(BigDecimal.valueOf(detalles.size())).toPlainString();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cuerpo.getBytes(StandardCharsets.UTF_8)));
        String contenido = "H," + identificador + ",2026-10-31,2026-10-31T18:00:00," + detalles.size() + "," + total
                + "\n" + cuerpo + "\nT," + detalles.size() + "," + total + "," + hash;
        return recaudaciones.procesar(identificador + ".csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = ArchivoRecaudacionRepository.class)
    @Import({ServiceRecaudacionesImpl.class, LectorArchivoRecaudacion.class, ValidadorRegistroRecaudacion.class})
    static class Configuracion {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(System.getenv("RECAUDACIONES_TEST_URL"), "postgres", "");
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var fabrica = new LocalContainerEntityManagerFactoryBean();
            fabrica.setDataSource(dataSource);
            fabrica.setPackagesToScan(ArchivoRecaudacion.class.getPackageName());
            fabrica.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            fabrica.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate", "hibernate.globally_quoted_identifiers", "true"));
            return fabrica;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory fabrica) {
            return new JpaTransactionManager(fabrica);
        }

        @Bean
        ServiceFechaNegocio fechaNegocio() {
            return () -> LocalDate.of(2026, 10, 31);
        }

        @Bean
        ServicePagos pagos() {
            return mock(ServicePagos.class);
        }
    }
}
