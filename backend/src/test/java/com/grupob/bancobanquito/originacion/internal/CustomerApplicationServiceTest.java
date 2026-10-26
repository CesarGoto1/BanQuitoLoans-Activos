package com.grupob.bancobanquito.originacion.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.originacion.ServiceSolicitudes;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("simulated")
@Transactional
class CustomerApplicationServiceTest {

    @Autowired
    ServiceSolicitudCliente customerApplicationService;

    @Autowired
    ServiceSolicitudes servicioSolicitudes;

    @Test
    void registersAnApplicationThatPassesTheEvaluation() {
        Solicitud application = customerApplicationService.register(1, request("10000.00", 12, MetodoAmortizacion.FRENCH));

        assertThat(application.getStatus()).isEqualTo(EstadoSolicitud.REGISTERED);
        assertThat(application.getReferenceInstallment()).isLessThan(new BigDecimal("920.00"));
        assertThat(application.getResolutionDate()).isNull();
        assertThat(application.getRejectionReason()).isNull();
    }

    @Test
    void rejectsAGermanApplicationWhoseFirstInstallmentExceedsTheLimit() {
        Solicitud application = customerApplicationService.register(1, request("10000.00", 12, MetodoAmortizacion.GERMAN));

        assertThat(application.getStatus()).isEqualTo(EstadoSolicitud.REJECTED);
        assertThat(application.getReferenceInstallment()).isGreaterThan(new BigDecimal("920.00"));
        assertThat(application.getRejectionReason()).contains("920,00");
        assertThat(application.getResolutionDate()).isNotNull();
    }

    @Test
    void doesNotRegisterAnAmountOutOfRange() {
        assertThatThrownBy(() -> customerApplicationService.register(1, request("40000.00", 12, MetodoAmortizacion.FRENCH)))
                .isInstanceOf(ExcepcionOriginacion.class)
                .hasMessageContaining("30.000,00")
                .extracting(e -> ((ExcepcionOriginacion) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void doesNotRegisterASecondPendingApplication() {
        customerApplicationService.register(2, request("5000.00", 24, MetodoAmortizacion.FRENCH));

        assertThatThrownBy(() -> customerApplicationService.register(2, request("3000.00", 12, MetodoAmortizacion.FRENCH)))
                .isInstanceOf(ExcepcionOriginacion.class)
                .hasMessageContaining("pendiente");
    }

    @Test
    @Sql(statements = """
            INSERT INTO "operaciones" ("idSolicitud", "idCliente", "cedulaCliente", "sistemaAmortizacion", "numeroOperacion",
                "montoAprobado", "teaAsignada", "plazoMeses", "seguroMensual", "contribucionSolca", "montoNeto",
                "cuentaDestino", "fechaDesembolso", "estadoInstruccionDesembolso", "estadoCartera", "saldoCapital",
                "diasMora", "usuarioCreacion")
            VALUES (9003, 3, '1709876543', 'FRANCES', 'OP-PRUEBA-MORA', 30000.00, 0.1550, 60, 18.00, 150.00, 29850.00,
                '2200000003', DATE '2026-10-31', 'ENTREGADA', 'VENCIDA', 30000.00, 5, 'pruebas');
            """)
    void doesNotRegisterWhenTheCustomerHasDaysPastDue() {
        assertThatThrownBy(() -> customerApplicationService.register(3, request("1000.00", 12, MetodoAmortizacion.FRENCH)))
                .isInstanceOf(ExcepcionOriginacion.class)
                .hasMessageContaining("mora");
    }

    @Test
    void doesNotRegisterForAnUnknownCustomer() {
        assertThatThrownBy(() -> customerApplicationService.register(77, request("1000.00", 12, MetodoAmortizacion.FRENCH)))
                .isInstanceOf(ExcepcionOriginacion.class)
                .extracting(e -> ((ExcepcionOriginacion) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listsOnlyTheApplicationsOfTheCustomer() {
        customerApplicationService.register(1, request("10000.00", 12, MetodoAmortizacion.FRENCH));

        assertThat(customerApplicationService.findByCustomer(1, null, PageRequest.of(0, 10)))
                .allMatch(application -> application.getCustomerId().equals(1))
                .hasSize(2);
        assertThat(customerApplicationService.findByCustomer(1, EstadoSolicitud.REGISTERED, PageRequest.of(0, 10)))
                .hasSize(1);
    }

    @Test
    void findsOnlyApprovedApplicationsForTheDisbursement() {
        Solicitud registered = customerApplicationService.register(1, request("10000.00", 12, MetodoAmortizacion.FRENCH));

        assertThat(servicioSolicitudes.findApproved(9001)).isPresent();
        assertThat(servicioSolicitudes.findApproved(registered.getId())).isEmpty();
    }

    @Test
    void simulatesALoanWithTheReferenceRateWithoutSavingIt() {
        SimulacionPrestamo simulation = customerApplicationService.simulate(
                new PeticionSimulacionPrestamo(new BigDecimal("10000.00"), 12, MetodoAmortizacion.GERMAN));

        assertThat(simulation.teaAsignada()).isEqualByComparingTo("0.1550");
        assertThat(simulation.seguroMensual()).isEqualByComparingTo("6.00");
        assertThat(simulation.contribucionSolca()).isEqualByComparingTo("50.00");
        assertThat(simulation.montoNeto()).isEqualByComparingTo("9950.00");
        assertThat(simulation.cuotas()).hasSize(12);
        assertThat(simulation.primeraCuota()).isGreaterThan(simulation.ultimaCuota());
        assertThat(simulation.costoTotal()).isEqualByComparingTo(
                simulation.interesTotal().add(new BigDecimal("72.00")).add(new BigDecimal("50.00")));
        assertThat(customerApplicationService.findByCustomer(1, EstadoSolicitud.REGISTERED, PageRequest.of(0, 10)))
                .isEmpty();
    }

    @Test
    void doesNotSimulateATermOutOfRange() {
        assertThatThrownBy(() -> customerApplicationService.simulate(
                new PeticionSimulacionPrestamo(new BigDecimal("10000.00"), 3, MetodoAmortizacion.FRENCH)))
                .isInstanceOf(ExcepcionOriginacion.class)
                .hasMessageContaining("entre 6 y 60 meses");
    }

    @Test
    void returnsTheCreditLimitsInForce() {
        LimitesCredito limits = customerApplicationService.findCreditLimits();

        assertThat(limits.montoMinimo()).isEqualByComparingTo("500.00");
        assertThat(limits.montoMaximo()).isEqualByComparingTo("30000.00");
        assertThat(limits.plazoMinimoMeses()).isEqualTo(6);
        assertThat(limits.plazoMaximoMeses()).isEqualTo(60);
    }

    private PeticionSolicitud request(String monto, Integer plazoMeses, MetodoAmortizacion metodo) {
        return new PeticionSolicitud(new BigDecimal(monto), plazoMeses, metodo, DestinoCredito.HEALTH,
                new BigDecimal("3500.00"), new BigDecimal("1200.00"));
    }
}
