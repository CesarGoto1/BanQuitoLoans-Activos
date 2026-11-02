package com.grupob.bancobanquito.cierre.internal;

import com.grupob.bancobanquito.cierre.ResultadoCierreDia;
import com.grupob.bancobanquito.cierre.ServiceCierreDia;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analista/cierres-dia")
@Tag(name = "Day closing")
class ControllerCierreDia {

    private final ServiceCierreDia closingService;

    ControllerCierreDia(ServiceCierreDia closingService) {
        this.closingService = closingService;
    }

    @PostMapping
    @Operation(summary = "Runs the end-of-day closing of the current business date; a failed closing resumes from the stage that did not finish")
    ResultadoCierreDia close(@RequestHeader("X-Identificacion") String identificacion,
            @Valid @RequestBody PeticionCierreDia request) {
        return closingService.ejecutarCierre(request.fechaNegocio(), identificacion.trim());
    }

    @GetMapping
    @Operation(summary = "Lists the day closings with the log of their stages, newest business date first")
    Page<ResultadoCierreDia> findAll(
            @PageableDefault(size = 10, sort = "fechaNegocio", direction = Sort.Direction.DESC) Pageable paginacion) {
        return closingService.listarCierres(paginacion);
    }

    @GetMapping("/{fechaNegocio}")
    @Operation(summary = "Returns the state of a day closing and the start, end, duration and result of each stage")
    ResultadoCierreDia findByDate(
            @PathVariable("fechaNegocio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNegocio) {
        return closingService.consultarCierre(fechaNegocio);
    }
}
