package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.acceso.internal.Analista;
import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class SolicitudesDePrueba {
    public static final LocalDate FECHA = LocalDate.of(2026, 10, 31);

    private SolicitudesDePrueba() {
    }

    public static Integer clienteIndependiente(EntityManager entityManager) {
        int cliente;
        do {
            cliente = (int) (Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 1000000000L) + 1000000000L);
        } while (entityManager.createQuery("select count(s) from Solicitud s where s.idCliente = :cliente", Long.class)
                .setParameter("cliente", cliente).getSingleResult() != 0
                || entityManager.createQuery("select count(p) from PrestamoCliente p where p.idCliente = :cliente", Long.class)
                .setParameter("cliente", cliente).getSingleResult() != 0);
        return cliente;
    }

    public static Integer crearAprobada(EntityManager entityManager, Integer cliente, String monto,
                                        int plazo, MetodoAmortizacion sistema) {
        Analista analista = new Analista();
        String cedula;
        do {
            cedula = String.format("%010d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10000000000L));
        } while (entityManager.createQuery("select count(a) from Analista a where a.cedula = :cedula", Long.class)
                .setParameter("cedula", cedula).getSingleResult() != 0);
        analista.setCedula(cedula);
        analista.setNombre("Analista de escenario aislado");
        analista.setCreatedBy("pruebas-aislamiento");
        analista.setCreatedAt(LocalDateTime.now());
        entityManager.persist(analista);
        Solicitud solicitud = new Solicitud();
        solicitud.setCustomerId(cliente);
        solicitud.setAmortizationMethod(sistema);
        solicitud.setAnalystId(analista.getId());
        solicitud.setRequestedAmount(new BigDecimal(monto));
        solicitud.setTermMonths(plazo);
        solicitud.setPurpose(DestinoCredito.SALUD);
        solicitud.setMonthlyIncome(new BigDecimal("10000.00"));
        solicitud.setMonthlyExpenses(new BigDecimal("1000.00"));
        solicitud.setApplicationDate(FECHA);
        solicitud.setRegisteredAt(LocalDateTime.now());
        solicitud.setReferenceInstallment(new BigDecimal("100.00"));
        solicitud.setAssignedRate(new BigDecimal("0.1550"));
        solicitud.setFinalInstallment(new BigDecimal("100.00"));
        solicitud.setStatus(EstadoSolicitud.APROBADA);
        solicitud.setResolutionDate(FECHA);
        solicitud.setCreatedBy("pruebas-aislamiento");
        solicitud.setCreatedAt(LocalDateTime.now());
        entityManager.persist(solicitud);
        entityManager.flush();
        return solicitud.getId();
    }

    public static void marcarRegistrada(EntityManager entityManager, Integer idSolicitud) {
        Solicitud solicitud = entityManager.find(Solicitud.class, idSolicitud);
        if (solicitud == null || !"pruebas-aislamiento".equals(solicitud.getCreatedBy())) {
            throw new IllegalArgumentException("La solicitud no pertenece al escenario de pruebas.");
        }
        solicitud.setStatus(EstadoSolicitud.REGISTRADA);
        solicitud.setAssignedRate(null);
        solicitud.setFinalInstallment(null);
        solicitud.setResolutionDate(null);
        entityManager.flush();
    }

    public static void eliminarPreparacion(EntityManager entityManager, Integer idSolicitud) {
        Solicitud solicitud = entityManager.find(Solicitud.class, idSolicitud);
        if (solicitud == null || !"pruebas-aislamiento".equals(solicitud.getCreatedBy())) {
            throw new IllegalArgumentException("La solicitud no pertenece al escenario de pruebas.");
        }
        Analista analista = entityManager.find(Analista.class, solicitud.getAnalystId());
        entityManager.remove(solicitud);
        entityManager.flush();
        entityManager.remove(analista);
    }
}
