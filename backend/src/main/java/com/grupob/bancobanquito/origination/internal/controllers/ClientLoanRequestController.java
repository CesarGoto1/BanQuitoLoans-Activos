package com.grupob.bancobanquito.origination.internal.controllers;
import com.grupob.bancobanquito.origination.internal.entities.*;
import com.grupob.bancobanquito.origination.internal.repositories.*;
import com.grupob.bancobanquito.origination.internal.services.*;
import com.grupob.bancobanquito.origination.internal.controllers.*;
import com.grupob.bancobanquito.origination.internal.exceptions.*;
import com.grupob.bancobanquito.access.AccessService;
import com.grupob.bancobanquito.access.Identity;
import com.grupob.bancobanquito.origination.LoanRequestService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/client/requests")
public class ClientLoanRequestController {

    private final LoanRequestService solicitudService;
    private final AccessService accesoService;

    public ClientLoanRequestController(LoanRequestService solicitudService, AccessService accesoService) {
        this.solicitudService = solicitudService;
        this.accesoService = accesoService;
    }

    
    @GetMapping
    public List<LoanRequestResponse> fetch(@RequestParam(required = false) String estado) {
        Identity identidad = accesoService.currentIdentity();
        return solicitudService.findByClient(identidad.identificacion(), estado).stream()
                .map(LoanRequestResponse::from)
                .toList();
    }
}
















