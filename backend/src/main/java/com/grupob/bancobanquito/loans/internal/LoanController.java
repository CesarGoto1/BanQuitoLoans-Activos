package com.grupob.bancobanquito.loans.internal;

import com.grupob.bancobanquito.loans.ConsultaPrestamoService;
import com.grupob.bancobanquito.loans.EstadoCartera;
import com.grupob.bancobanquito.loans.ResumenCuota;
import com.grupob.bancobanquito.loans.ResumenPrestamo;
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
@RequestMapping("/api/loans")
@Tag(name = "Loans")
class LoanController {

    private static final String CUSTOMER = "CLIENTE";

    private final ConsultaPrestamoService consultaPrestamoService;

    LoanController(ConsultaPrestamoService consultaPrestamoService) {
        this.consultaPrestamoService = consultaPrestamoService;
    }

    @GetMapping("/{loanNumber}")
    @Operation(summary = "Préstamo buscado por su número de operación, con su estado de cartera, días de mora y saldo de capital")
    ResumenPrestamo buscarPorNumeroOperacion(@PathVariable String loanNumber,
                                             @RequestHeader("X-Rol") String role,
                                             @RequestHeader("X-Identificacion") String nationalId) {
        return visibleLoan(loanNumber, role, nationalId);
    }

    @GetMapping
    @Operation(summary = "Loans of one customer, filtered by national ID and optionally by portfolio status, one page at a time")
    Page<ResumenPrestamo> findByCustomer(
            @RequestHeader("X-Rol") String role,
            @RequestHeader("X-Identificacion") String nationalId,
            @RequestParam(required = false) @Pattern(regexp = "\\d{10}", message = "La cédula debe tener 10 dígitos.") String customerNationalId,
            @RequestParam(required = false) EstadoCartera portfolioStatus,
            @PageableDefault(size = 10, sort = "disbursementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        String customer = CUSTOMER.equals(role) ? nationalId : customerNationalId;
        if (customer == null) {
            throw new LoanException(HttpStatus.BAD_REQUEST, "El parámetro customerNationalId es obligatorio.");
        }
        return consultaPrestamoService.buscarPorCliente(customer, portfolioStatus, pageable);
    }

    @GetMapping("/{loanNumber}/installments")
    @Operation(summary = "Amortization schedule of a loan, one page at a time")
    Page<ResumenCuota> findInstallments(
            @PathVariable String loanNumber,
            @RequestHeader("X-Rol") String role,
            @RequestHeader("X-Identificacion") String nationalId,
            @PageableDefault(size = 12, sort = "installmentNumber") Pageable pageable) {
        visibleLoan(loanNumber, role, nationalId);
        return consultaPrestamoService.buscarTablaAmortizacion(loanNumber, pageable);
    }

    private ResumenPrestamo visibleLoan(String loanNumber, String role, String nationalId) {
        ResumenPrestamo loan = consultaPrestamoService.buscarPorNumeroOperacion(loanNumber);
        if (CUSTOMER.equals(role) && !loan.cedulaCliente().equals(nationalId)) {
            throw new LoanException(HttpStatus.NOT_FOUND, "La operación " + loanNumber + " no existe.");
        }
        return loan;
    }
}
