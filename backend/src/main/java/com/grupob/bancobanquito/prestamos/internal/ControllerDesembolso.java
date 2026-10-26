package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.prestamos.VistaPreviaDesembolso;
import com.grupob.bancobanquito.prestamos.ResultadoDesembolso;
import com.grupob.bancobanquito.prestamos.ServicePrestamo;
import com.grupob.bancobanquito.prestamos.DesembolsoPendiente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Disbursements")
class ControllerDesembolso {

    private final ServicePrestamo loanService;

    ControllerDesembolso(ServicePrestamo loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/desembolsos-pendientes")
    @Operation(summary = "Lists approved loan applications that have not been disbursed yet, oldest resolution first")
    Page<DesembolsoPendiente> findPending(
            @PageableDefault(size = 10, sort = "fechaResolucion") Pageable paginacion) {
        return loanService.findPendingDisbursements(paginacion);
    }

    @GetMapping("/vistas-previas-desembolso/{idSolicitud}")
    @Operation(summary = "Calculates insurance, SOLCA contribution and net amount of an approved loan application without saving anything")
    VistaPreviaDesembolso preview(@PathVariable("idSolicitud") Integer idSolicitud) {
        return loanService.previewDisbursement(idSolicitud);
    }

    @PostMapping("/desembolsos")
    @Operation(summary = "Disburses an approved loan application; repeating the same request returns the existing disbursement")
    ResponseEntity<ResultadoDesembolso> disburse(@Valid @RequestBody PeticionDesembolso request) {
        Optional<ResultadoDesembolso> existing = loanService.findDisbursement(request.idSolicitud());
        if (existing.isPresent()) {
            if (!existing.get().cuentaDestino().equals(request.cuentaDestino())) {
                throw new ExcepcionPrestamo(HttpStatus.CONFLICT, "La solicitud " + request.idSolicitud()
                        + " ya fue desembolsada en la cuenta " + existing.get().cuentaDestino() + ".");
            }
            return ResponseEntity.ok(existing.get());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.disburse(request.idSolicitud(), request.cuentaDestino()));
    }
}
