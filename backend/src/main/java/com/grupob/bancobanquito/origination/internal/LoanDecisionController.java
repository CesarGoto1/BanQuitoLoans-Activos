package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.origination.LoanDecisionResult;
import com.grupob.bancobanquito.origination.LoanDecisionService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analista/solicitudes")
class LoanDecisionController {

    private final LoanDecisionService decisionService;

    LoanDecisionController(LoanDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @PostMapping("/{idSolicitud}/aprobacion")
    ResponseEntity<LoanDecisionResult> approveApplication(@PathVariable("idSolicitud") Integer applicationId,
            @RequestHeader(name = "X-Rol", required = false) String role,
            @RequestHeader(name = "X-Identificacion", required = false) String identification,
            @Valid @RequestBody ApproveLoanRequest request) {
        validateHeaders(role, identification);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(decisionService.approveApplication(applicationId, identification.trim(), request.annualEffectiveRate()));
    }

    @PostMapping("/{idSolicitud}/rechazo")
    ResponseEntity<LoanDecisionResult> rejectApplication(@PathVariable("idSolicitud") Integer applicationId,
            @RequestHeader(name = "X-Rol", required = false) String role,
            @RequestHeader(name = "X-Identificacion", required = false) String identification,
            @Valid @RequestBody RejectLoanRequest request) {
        validateHeaders(role, identification);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(decisionService.rejectApplication(applicationId, identification.trim(), request.rejectionReason()));
    }

    private void validateHeaders(String role, String identification) {
        if (role == null || !"ANALISTA".equalsIgnoreCase(role.trim())) {
            throw new AccesoDenegadoAnalistaException("Acceso denegado: se requiere la cabecera X-Rol con valor ANALISTA.");
        }
        if (identification == null || !identification.trim().matches("[0-9]{10}")) {
            throw new CabeceraFaltanteException("La cabecera X-Identificacion debe contener una identificación de 10 dígitos.");
        }
    }
}
