package com.grupob.bancobanquito.loans.internal;

import com.grupob.bancobanquito.loans.DisbursementPreview;
import com.grupob.bancobanquito.loans.DisbursementResult;
import com.grupob.bancobanquito.loans.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
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
class DisbursementController {

    private final LoanService loanService;

    DisbursementController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/disbursement-previews/{applicationId}")
    @Operation(summary = "Calculates insurance, SOLCA contribution and net amount of an approved loan application without saving anything")
    DisbursementPreview preview(@PathVariable Integer applicationId) {
        return loanService.previewDisbursement(applicationId);
    }

    @PostMapping("/disbursements")
    @Operation(summary = "Disburses an approved loan application; repeating the same request returns the existing disbursement")
    ResponseEntity<DisbursementResult> disburse(@Valid @RequestBody DisbursementRequest request) {
        Optional<DisbursementResult> existing = loanService.findDisbursement(request.applicationId());
        if (existing.isPresent()) {
            if (!existing.get().destinationAccount().equals(request.destinationAccount())) {
                throw new LoanException(HttpStatus.CONFLICT, "La solicitud " + request.applicationId()
                        + " ya fue desembolsada en la cuenta " + existing.get().destinationAccount() + ".");
            }
            return ResponseEntity.ok(existing.get());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.disburse(request.applicationId(), request.destinationAccount()));
    }
}
