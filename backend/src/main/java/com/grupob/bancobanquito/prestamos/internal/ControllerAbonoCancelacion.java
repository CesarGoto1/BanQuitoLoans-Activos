package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.prestamos.LiquidacionCancelacion;
import com.grupob.bancobanquito.prestamos.ResultadoAbono;
import com.grupob.bancobanquito.prestamos.ServiceAbonoExtraordinario;
import com.grupob.bancobanquito.prestamos.ServiceCancelacionAnticipada;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analista/operaciones/{numeroOperacion}")
@Tag(name = "Abonos y cancelaciones")
class ControllerAbonoCancelacion {

    private final ServiceAbonoExtraordinario servicioAbono;
    private final ServiceCancelacionAnticipada servicioCancelacion;

    ControllerAbonoCancelacion(ServiceAbonoExtraordinario servicioAbono,
                               ServiceCancelacionAnticipada servicioCancelacion) {
        this.servicioAbono = servicioAbono;
        this.servicioCancelacion = servicioCancelacion;
    }

    @PostMapping("/simulaciones-abono")
    @Operation(summary = "Calcula el abono extraordinario y el cuadro antes y después sin guardar nada")
    ResultadoAbono simularAbono(@PathVariable("numeroOperacion") String numeroOperacion,
                                @Valid @RequestBody PeticionAbono peticion) {
        return servicioAbono.simular(numeroOperacion, peticion.monto(), peticion.modalidad());
    }

    @PostMapping("/abonos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra el abono extraordinario, recalcula la tabla y genera los asientos")
    ResultadoAbono registrarAbono(@PathVariable("numeroOperacion") String numeroOperacion,
                                  @RequestHeader("X-Identificacion") String cedulaAnalista,
                                  @Valid @RequestBody PeticionAbono peticion) {
        return servicioAbono.registrar(numeroOperacion, peticion.monto(), peticion.modalidad(), cedulaAnalista);
    }

    @GetMapping("/valor-cancelacion")
    @Operation(summary = "Valor de cancelación anticipada a la fecha de negocio, desglosado por rubro")
    LiquidacionCancelacion consultarCancelacion(@PathVariable("numeroOperacion") String numeroOperacion) {
        return servicioCancelacion.consultar(numeroOperacion);
    }

    @PostMapping("/cancelaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra el pago de la cancelación anticipada y deja la operación en estado CANCELADA")
    LiquidacionCancelacion registrarCancelacion(@PathVariable("numeroOperacion") String numeroOperacion,
                                                @RequestHeader("X-Identificacion") String cedulaAnalista,
                                                @Valid @RequestBody PeticionCancelacion peticion) {
        return servicioCancelacion.registrar(numeroOperacion, peticion.montoRecibido(), cedulaAnalista);
    }
}
