package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.prestamos.ConsultaPrestamoService;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.ResumenCuota;
import com.grupob.bancobanquito.prestamos.ResumenPrestamo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prestamos")
@Tag(name = "Loans")
class ControllerPrestamo {

    private static final String CUSTOMER = "CLIENTE";

    private final ConsultaPrestamoService consultaPrestamoService;

    ControllerPrestamo(ConsultaPrestamoService consultaPrestamoService) {
        this.consultaPrestamoService = consultaPrestamoService;
    }

    @GetMapping("/{numeroOperacion}")
    @Operation(summary = "Préstamo buscado por su número de operación, con su estado de cartera, días de mora y saldo de capital")
    ResumenPrestamo buscarPorNumeroOperacion(@PathVariable("numeroOperacion") String numeroOperacion,
                                             @RequestHeader("X-Rol") String role,
                                             @RequestHeader("X-Identificacion") String cedula) {
        return visibleLoan(numeroOperacion, role, cedula);
    }

    @GetMapping
    @Operation(summary = "Loans of one customer, filtered by national ID and optionally by portfolio status, one page at a time")
    Page<ResumenPrestamo> findByCustomer(
            @RequestHeader("X-Rol") String role,
            @RequestHeader("X-Identificacion") String cedula,
            @RequestParam(value = "cedulaCliente", required = false) @Pattern(regexp = "\\d{10}", message = "La cédula debe tener 10 dígitos.") String cedulaCliente,
            @RequestParam(required = false) EstadoCartera estadoCartera,
            @PageableDefault(size = 10, sort = "fechaDesembolso", direction = Sort.Direction.DESC) Pageable paginacion) {
        String customer = CUSTOMER.equals(role) ? cedula : cedulaCliente;
        if (customer == null) {
            throw new ExcepcionPrestamo(HttpStatus.BAD_REQUEST, "El parámetro cedulaCliente es obligatorio.");
        }
        return consultaPrestamoService.buscarPorCliente(customer, estadoCartera, paginacion);
    }

    @GetMapping("/{numeroOperacion}/cuotas")
    @Operation(summary = "Amortization schedule of a loan, one page at a time")
    Page<ResumenCuota> findInstallments(
            @PathVariable("numeroOperacion") String numeroOperacion,
            @RequestHeader("X-Rol") String role,
            @RequestHeader("X-Identificacion") String cedula,
            @PageableDefault(size = 12, sort = "numeroCuota") Pageable paginacion) {
        visibleLoan(numeroOperacion, role, cedula);
        return consultaPrestamoService.buscarTablaAmortizacion(numeroOperacion, paginacion);
    }

    private ResumenPrestamo visibleLoan(String numeroOperacion, String role, String cedula) {
        ResumenPrestamo loan = consultaPrestamoService.buscarPorNumeroOperacion(numeroOperacion);
        if (CUSTOMER.equals(role) && !loan.cedulaCliente().equals(cedula)) {
            throw new ExcepcionPrestamo(HttpStatus.NOT_FOUND, "La operación " + numeroOperacion + " no existe.");
        }
        return loan;
    }
}
