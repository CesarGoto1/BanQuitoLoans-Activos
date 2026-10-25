package com.grupob.bancobanquito.loans.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.loans.ConsultaPrestamoService;
import com.grupob.bancobanquito.loans.EstadoCartera;
import com.grupob.bancobanquito.loans.LoanService;
import com.grupob.bancobanquito.loans.ResumenCuota;
import com.grupob.bancobanquito.loans.ResumenPrestamo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("simulated")
@Transactional
class ConsultaPrestamoServiceTest {

    @Autowired
    LoanService loanService;

    @Autowired
    ConsultaPrestamoService consultaPrestamoService;

    @Test
    void buscaUnPrestamoPorSuNumeroDeOperacion() {
        loanService.disburse(9002, "2200000002");

        ResumenPrestamo prestamo = consultaPrestamoService.buscarPorNumeroOperacion("OP-20261031-009002");

        assertThat(prestamo.cedulaCliente()).isEqualTo("1712345675");
        assertThat(prestamo.montoAprobado()).isEqualByComparingTo("5000.00");
        assertThat(prestamo.saldoCapital()).isEqualByComparingTo("5000.00");
        assertThat(prestamo.diasMora()).isZero();
        assertThat(prestamo.estadoCartera()).isEqualTo(EstadoCartera.POR_VENCER);
    }

    @Test
    void respondeNoEncontradoSiElNumeroDeOperacionNoExiste() {
        assertThatThrownBy(() -> consultaPrestamoService.buscarPorNumeroOperacion("OP-20261031-999999"))
                .isInstanceOf(LoanException.class)
                .extracting(e -> ((LoanException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void buscaLosPrestamosDeUnClientePorPaginas() {
        loanService.disburse(9003, "2200000003");

        Page<ResumenPrestamo> prestamos = consultaPrestamoService.buscarPorCliente("1709876543", null,
                PageRequest.of(0, 10));
        assertThat(prestamos.getContent()).extracting(ResumenPrestamo::numeroOperacion)
                .containsExactly("OP-20261031-009003");

        Page<ResumenCuota> cuotas = consultaPrestamoService.buscarTablaAmortizacion("OP-20261031-009003",
                PageRequest.of(0, 12));
        assertThat(cuotas.getContent()).hasSize(12);
        assertThat(cuotas.getTotalElements()).isEqualTo(60);
    }

    @Test
    void filtraLosPrestamosDeUnClientePorEstadoDeCartera() {
        loanService.disburse(9003, "2200000003");

        assertThat(consultaPrestamoService.buscarPorCliente("1709876543", EstadoCartera.POR_VENCER,
                PageRequest.of(0, 10)).getContent()).hasSize(1);
        assertThat(consultaPrestamoService.buscarPorCliente("1709876543", EstadoCartera.VENCIDA,
                PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    void respondeNoEncontradoSiLaOperacionDeLaTablaNoExiste() {
        assertThatThrownBy(() -> consultaPrestamoService.buscarTablaAmortizacion("OP-20261031-999999",
                PageRequest.of(0, 12)))
                .isInstanceOf(LoanException.class)
                .extracting(e -> ((LoanException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
