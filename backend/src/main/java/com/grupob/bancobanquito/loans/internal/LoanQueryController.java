package com.grupob.bancobanquito.loans.internal;

import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/clients/{clientId}/operations")
class LoanQueryController {

    private final LoanQueryService loanQueryService;

    LoanQueryController(LoanQueryService loanQueryService) {
        this.loanQueryService = loanQueryService;
    }

    @GetMapping
    List<LoanQueryService.OperationResponse> getClientOperations(
            @PathVariable @Positive Integer clientId) {
        return loanQueryService.findOperations(clientId);
    }

    @GetMapping(value = "/report", produces = "text/csv")
    ResponseEntity<byte[]> downloadClientOperationsReport(
            @PathVariable @Positive Integer clientId) {
        byte[] content = loanQueryService.buildReport(clientId).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("client-operations-report.csv", StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
