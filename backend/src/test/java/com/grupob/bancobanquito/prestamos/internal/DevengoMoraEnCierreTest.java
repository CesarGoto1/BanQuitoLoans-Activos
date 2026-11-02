package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupob.bancobanquito.cierre.EjecucionEtapaCierre;
import com.grupob.bancobanquito.cierre.PasoCierre;
import com.grupob.bancobanquito.cierre.ResultadoCierreDia;
import com.grupob.bancobanquito.cierre.ServiceCierreDia;
import com.grupob.bancobanquito.prestamos.ServiceDevengoMora;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Sql("/test-data-pagos.sql")
class DevengoMoraEnCierreTest {

    private static final String ANALISTA = "1700000001";
    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 10, 31);

    @Autowired
    ServiceCierreDia servicioCierre;

    @Autowired
    ServiceDevengoMora servicioDevengoMora;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void elCierreDelDiaSiguienteAlDesembolsoDevengaUnDiaYNoTocaCanceladasNiCastigadas() {
        ResultadoCierreDia diaDesembolso = servicioCierre.ejecutarCierre(DESEMBOLSO, ANALISTA);
        ResultadoCierreDia diaSiguiente = servicioCierre.ejecutarCierre(DESEMBOLSO.plusDays(1), ANALISTA);

        assertThat(diaDesembolso.estadoCierre()).isEqualTo("COMPLETADO");
        assertThat(diaSiguiente.estadoCierre()).isEqualTo("COMPLETADO");
        assertThat(devengos(9101, DESEMBOLSO)).isZero();
        assertThat(devengos(9101, DESEMBOLSO.plusDays(1))).isEqualTo(1);
        assertThat(devengos(9104, DESEMBOLSO.plusDays(1))).isEqualTo(1);
        assertThat(devengos(9102, DESEMBOLSO.plusDays(1))).isZero();
        assertThat(devengos(9103, DESEMBOLSO.plusDays(1))).isZero();
        assertThat(jdbc.queryForObject("""
                select "interesDevengado" from "devengosDiarios"
                where "idOperacion" = 9101 and "fechaDevengo" = ?""", BigDecimal.class, DESEMBOLSO.plusDays(1)))
                .isBetween(new BigDecimal("4.30"), new BigDecimal("4.40"));
        EjecucionEtapaCierre devengo = ejecucion(diaSiguiente, PasoCierre.DEVENGO);
        assertThat(devengo.estadoEtapa()).isEqualTo("EXITOSA");
        assertThat(devengo.registrosProcesados()).isGreaterThanOrEqualTo(2);
        assertThat(ejecucion(diaSiguiente, PasoCierre.MORA).estadoEtapa()).isEqualTo("EXITOSA");
    }

    @Test
    void laMoraCorreDesdeElDiaSiguienteAlVencimiento() {
        LocalDate vencimiento = LocalDate.of(2026, 11, 30);
        registrarDia(vencimiento);
        registrarDia(vencimiento.plusDays(1));

        servicioDevengoMora.ejecutarMora(vencimiento);
        assertThat(moras(9104, 1, vencimiento)).isZero();

        servicioDevengoMora.ejecutarMora(vencimiento.plusDays(1));
        assertThat(moras(9104, 1, vencimiento.plusDays(1))).isEqualTo(1);
        assertThat(moras(9104, 2, vencimiento.plusDays(1))).isZero();
    }

    private void registrarDia(LocalDate fecha) {
        jdbc.update("""
                insert into "cierresDia" ("fechaNegocio", "estadoCierre", "usuarioCreacion")
                values (?, 'COMPLETADO', 'pruebas') on conflict ("fechaNegocio") do nothing""", fecha);
    }

    private int devengos(int idOperacion, LocalDate fecha) {
        return jdbc.queryForObject("""
                select count(*) from "devengosDiarios" where "idOperacion" = ? and "fechaDevengo" = ?""",
                Integer.class, idOperacion, fecha);
    }

    private int moras(int idOperacion, int numeroCuota, LocalDate fecha) {
        return jdbc.queryForObject("""
                select count(*) from "morasDiarias" m join "cuotas" c on c."idCuota" = m."idCuota"
                where c."idOperacion" = ? and c."numeroCuota" = ? and m."fechaMora" = ?""",
                Integer.class, idOperacion, numeroCuota, fecha);
    }

    private static EjecucionEtapaCierre ejecucion(ResultadoCierreDia resultado, PasoCierre paso) {
        return resultado.ejecuciones().stream().filter(e -> e.etapa() == paso).findFirst().orElseThrow();
    }
}
