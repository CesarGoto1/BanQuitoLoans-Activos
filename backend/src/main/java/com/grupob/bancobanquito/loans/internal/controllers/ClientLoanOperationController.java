package com.grupob.bancobanquito.loans.internal.controllers;
import com.grupob.bancobanquito.loans.internal.entities.*;
import com.grupob.bancobanquito.loans.internal.repositories.*;
import com.grupob.bancobanquito.loans.internal.services.*;
import com.grupob.bancobanquito.loans.internal.controllers.*;
import com.grupob.bancobanquito.loans.internal.exceptions.*;
import com.grupob.bancobanquito.access.AccessService;
import com.grupob.bancobanquito.access.Identity;
import com.grupob.bancobanquito.loans.LoanOperationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/client/operations")
public class ClientLoanOperationController {

    private final LoanOperationService operacionService;
    private final AccessService accesoService;

    public ClientLoanOperationController(LoanOperationService operacionService, AccessService accesoService) {
        this.operacionService = operacionService;
        this.accesoService = accesoService;
    }

    
    @GetMapping
    public List<LoanOperationResponse> fetch() {
        Identity identidad = accesoService.currentIdentity();
        return operacionService.findByClient(identidad.identificacion()).stream()
                .map(LoanOperationResponse::from)
                .toList();
    }

    
    @GetMapping("/{numeroOperacion}/installments")
    public List<InstallmentResponse> findInstallments(@PathVariable String numeroOperacion) {
        Identity identidad = accesoService.currentIdentity();
        return operacionService.findInstallments(identidad.identificacion(), numeroOperacion).stream()
                .map(InstallmentResponse::from)
                .toList();
    }
}
















