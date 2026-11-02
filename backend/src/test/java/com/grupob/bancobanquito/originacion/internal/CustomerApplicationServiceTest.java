package com.grupob.bancobanquito.originacion.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.originacion.ServiceSolicitudes;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import jakarta.persistence.EntityManager;
import com.grupob.bancobanquito.pasivos.*;
import com.grupob.bancobanquito.prestamos.ServicePrestamo;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("simulated")
@Transactional
class CustomerApplicationServiceTest {

    @Autowired
    ServiceSolicitudCliente customerApplicationService;

    @Autowired
    ServiceSolicitudes servicioSolicitudes;

    @Autowired
    EntityManager entityManager;
    @Autowired
    ServicePrestamo prestamos;
    @MockitoBean
    ServicePasivos pasivos;
    @MockitoBean
    ServiceFechaNegocio fechaNegocio;
    private Integer cliente;
    private Integer otroCliente;
    private Integer clienteMora;

    @BeforeEach
    void prepararClientes() {
        cliente = SolicitudesDePrueba.clienteIndependiente(entityManager);
        do {
            otroCliente = SolicitudesDePrueba.clienteIndependiente(entityManager);
        } while (otroCliente.equals(cliente));
        do {
            clienteMora = SolicitudesDePrueba.clienteIndependiente(entityManager);
        } while (clienteMora.equals(cliente) || clienteMora.equals(otroCliente));
        for (Integer id : List.of(cliente, otroCliente, clienteMora)) {
            when(pasivos.findCustomer(id)).thenReturn(Optional.of(new Cliente(id, "1710034065", "Cliente", "Prueba")));
            when(pasivos.findActiveAccounts(id)).thenReturn(List.of(new Cuenta("2200000001", "AHORROS")));
        }
        when(fechaNegocio.getCurrentBusinessDate()).thenReturn(SolicitudesDePrueba.FECHA);
    }

    @Test
    void registersAnApplicationThatPassesTheEvaluation() {
        Solicitud application = customerApplicationService.register(cliente, request("10000.00", 12, MetodoAmortizacion.FRANCES));

        assertThat(application.getStatus()).isEqualTo(EstadoSolicitud.REGISTRADA);
        assertThat(application.getReferenceInstallment()).isLessThan(new BigDecimal("920.00"));
        assertThat(application.getResolutionDate()).isNull();
        assertThat(application.getRejectionReason()).isNull();
    }

    @Test
    void rejectsAGermanApplicationWhoseFirstInstallmentExceedsTheLimit() {
        Solicitud application = customerApplicationService.register(cliente, request("10000.00", 12, MetodoAmortizacion.ALEMAN));

        assertThat(application.getStatus()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(application.getReferenceInstallment()).isGreaterThan(new BigDecimal("920.00"));
        assertThat(application.getRejectionReason()).contains("920,00");
        assertThat(application.getResolutionDate()).isNotNull();
    }

    @Test
    void doesNotRegisterAnAmountOutOfRange() {
        assertThatThrownBy(() -> customerApplicationService.register(cliente, request("40000.00", 12, MetodoAmortizacion.FRANCES)))
                .isInstanceOf(OriginacionException.class)
                .hasMessageContaining("30.000,00")
                .extracting(e -> ((OriginacionException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void doesNotRegisterASecondPendingApplication() {
        customerApplicationService.register(otroCliente, request("5000.00", 24, MetodoAmortizacion.FRANCES));

        assertThatThrownBy(() -> customerApplicationService.register(otroCliente, request("3000.00", 12, MetodoAmortizacion.FRANCES)))
                .isInstanceOf(OriginacionException.class)
                .hasMessageContaining("pendiente");
    }

    @Test
    void doesNotRegisterWhenTheCustomerHasDaysPastDue() {
        Integer solicitud = SolicitudesDePrueba.crearAprobada(entityManager, clienteMora,
                "30000.00", 60, MetodoAmortizacion.FRANCES);
        prestamos.disburse(solicitud, "2200000001");
        entityManager.createQuery("update Prestamo p set p.daysPastDue = 5, p.portfolioStatus = :estado where p.idSolicitud = :id")
                .setParameter("estado", EstadoCartera.VENCIDA).setParameter("id", solicitud).executeUpdate();
        entityManager.clear();
        assertThatThrownBy(() -> customerApplicationService.register(clienteMora, request("1000.00", 12, MetodoAmortizacion.FRANCES)))
                .isInstanceOf(OriginacionException.class)
                .hasMessageContaining("mora");
    }

    @Test
    void doesNotRegisterForAnUnknownCustomer() {
        assertThatThrownBy(() -> customerApplicationService.register(77, request("1000.00", 12, MetodoAmortizacion.FRANCES)))
                .isInstanceOf(OriginacionException.class)
                .extracting(e -> ((OriginacionException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listsOnlyTheApplicationsOfTheCustomer() {
        customerApplicationService.register(otroCliente, request("5000.00", 24, MetodoAmortizacion.FRANCES));
        Set<Integer> anteriores = identificadoresDelCliente(cliente, null);
        Set<Integer> registradasAnteriores = identificadoresDelCliente(cliente, EstadoSolicitud.REGISTRADA);
        Solicitud creada = customerApplicationService.register(cliente, request("10000.00", 12, MetodoAmortizacion.FRANCES));

        Set<Integer> esperadas = new HashSet<>(anteriores);
        esperadas.add(creada.getId());
        Set<Integer> registradasEsperadas = new HashSet<>(registradasAnteriores);
        registradasEsperadas.add(creada.getId());
        assertThat(identificadoresDelCliente(cliente, null)).containsExactlyInAnyOrderElementsOf(esperadas);
        assertThat(identificadoresDelCliente(cliente, EstadoSolicitud.REGISTRADA))
                .containsExactlyInAnyOrderElementsOf(registradasEsperadas);
    }

    @Test
    void findsOnlyApprovedApplicationsForTheDisbursement() {
        Solicitud registered = customerApplicationService.register(cliente, request("10000.00", 12, MetodoAmortizacion.FRANCES));

        Integer aprobada = SolicitudesDePrueba.crearAprobada(entityManager, otroCliente,
                "10000.00", 12, MetodoAmortizacion.FRANCES);
        assertThat(servicioSolicitudes.findApproved(aprobada)).isPresent();
        assertThat(servicioSolicitudes.findApproved(registered.getId())).isEmpty();
    }

    @Test
    void simulatesALoanWithTheReferenceRateWithoutSavingIt() {
        Set<Integer> anteriores = identificadoresDelCliente(cliente, null);
        SimulacionPrestamo simulation = customerApplicationService.simulate(
                new PeticionSimulacionPrestamo(new BigDecimal("10000.00"), 12, MetodoAmortizacion.ALEMAN));

        assertThat(simulation.teaAsignada()).isEqualByComparingTo("0.1550");
        assertThat(simulation.seguroMensual()).isEqualByComparingTo("6.00");
        assertThat(simulation.contribucionSolca()).isEqualByComparingTo("50.00");
        assertThat(simulation.montoNeto()).isEqualByComparingTo("9950.00");
        assertThat(simulation.cuotas()).hasSize(12);
        assertThat(simulation.primeraCuota()).isGreaterThan(simulation.ultimaCuota());
        assertThat(simulation.costoTotal()).isEqualByComparingTo(
                simulation.interesTotal().add(new BigDecimal("72.00")).add(new BigDecimal("50.00")));
        assertThat(identificadoresDelCliente(cliente, null)).containsExactlyInAnyOrderElementsOf(anteriores);
    }

    @Test
    void doesNotSimulateATermOutOfRange() {
        assertThatThrownBy(() -> customerApplicationService.simulate(
                new PeticionSimulacionPrestamo(new BigDecimal("10000.00"), 3, MetodoAmortizacion.FRANCES)))
                .isInstanceOf(OriginacionException.class)
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

    private Set<Integer> identificadoresDelCliente(Integer cliente, EstadoSolicitud estado) {
        Set<Integer> identificadores = new HashSet<>();
        int pagina = 0;
        org.springframework.data.domain.Page<Solicitud> resultados;
        do {
            resultados = customerApplicationService.findByCustomer(cliente, estado,
                    PageRequest.of(pagina++, 50, org.springframework.data.domain.Sort.by("id")));
            assertThat(resultados).allMatch(solicitud -> solicitud.getCustomerId().equals(cliente));
            if (estado != null) {
                assertThat(resultados).allMatch(solicitud -> solicitud.getStatus() == estado);
            }
            resultados.forEach(solicitud -> assertThat(identificadores.add(solicitud.getId())).isTrue());
        } while (resultados.hasNext());
        return identificadores;
    }

    private PeticionSolicitud request(String monto, Integer plazoMeses, MetodoAmortizacion metodo) {
        return new PeticionSolicitud(new BigDecimal(monto), plazoMeses, metodo, DestinoCredito.SALUD,
                new BigDecimal("3500.00"), new BigDecimal("1200.00"));
    }
}
