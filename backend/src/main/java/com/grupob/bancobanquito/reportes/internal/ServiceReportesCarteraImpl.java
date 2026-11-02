package com.grupob.bancobanquito.reportes.internal;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.ConsultaPrestamoService;
import java.util.HashSet;
import com.grupob.bancobanquito.cierre.ServiceCierreDia;
import com.grupob.bancobanquito.cierre.PasoCierre;
import com.grupob.bancobanquito.cierre.ResultadoCierreDia;
import com.grupob.bancobanquito.reportes.CorteCartera;
import com.grupob.bancobanquito.reportes.ResultadoSaludCartera;
import com.grupob.bancobanquito.reportes.DatosOperacionReporte;
import com.grupob.bancobanquito.reportes.ResultadoConcentracion;
import com.grupob.bancobanquito.reportes.ResultadoComposicionCartera;
import com.grupob.bancobanquito.reportes.ResultadoMorosidad;
import com.grupob.bancobanquito.reportes.ResultadoRollRate;
import com.grupob.bancobanquito.reportes.ServiceReportesCartera;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.grupob.bancobanquito.cobranza.ServicioFotografiaCartera;
import com.grupob.bancobanquito.cobranza.ResumenFotoCartera;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class ServiceReportesCarteraImpl implements ServiceReportesCartera {

    private final ServicioFotografiaCartera servicioFotografia;

    private final ServiceCierreDia servicioCierre;

    private final ConsultaPrestamoService consultaPrestamo;

    ServiceReportesCarteraImpl(ServicioFotografiaCartera servicioFotografia, ServiceCierreDia servicioCierre,
                              ConsultaPrestamoService consultaPrestamo) {
        this.servicioFotografia = servicioFotografia;
        this.servicioCierre = servicioCierre;
        this.consultaPrestamo = consultaPrestamo;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoSaludCartera consultarSalud(LocalDate fechaCorte) {
        CorteCartera corte = consultarCorte(fechaCorte);
        var identificadores = corte.operaciones().stream().map(CorteCartera.Operacion::idOperacion).toList();
        if (identificadores.isEmpty()) {
            return calcularSalud(corte, List.of());
        }
        var originales = consultaPrestamo.buscarDatosPorIds(identificadores);
        if (originales == null) {
            throw new IllegalArgumentException("No se recibieron los datos originales de las operaciones.");
        }
        var esperados = new HashSet<>(identificadores);
        var recibidos = new HashSet<Integer>();
        List<DatosOperacionReporte> datos = new ArrayList<>();
        for (var original : originales) {
            if (original == null || !esperados.contains(original.idOperacion())
                    || !recibidos.add(original.idOperacion()) || original.idCliente() == null
                    || original.idCliente() <= 0 || original.montoAprobado() == null
                    || original.montoAprobado().signum() <= 0 || original.plazoMeses() == null
                    || original.plazoMeses() <= 0 || original.sistemaAmortizacion() == null) {
                throw new IllegalArgumentException("Los datos originales contienen operaciones inesperadas, duplicadas o incompletas.");
            }
            CalculosCartera.validarImporte(original.montoAprobado());
            datos.add(new DatosOperacionReporte(original.idOperacion(), original.idCliente(), original.montoAprobado(),
                    original.plazoMeses(), original.sistemaAmortizacion()));
        }
        if (!recibidos.equals(esperados)) {
            throw new IllegalArgumentException("Faltan datos originales para las operaciones de la fotografía.");
        }
        return calcularSalud(corte, datos);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoMorosidad consultarMorosidad(LocalDate fechaCorte) {
        return calcularMorosidad(consultarCorte(fechaCorte));
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoComposicionCartera consultarComposicion(LocalDate fechaCorte) {
        return calcularComposicion(consultarCorte(fechaCorte));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal consultarCobertura(LocalDate fechaCorte) {
        CorteCartera corte = consultarCorte(fechaCorte);
        ResultadoMorosidad morosidad = calcularMorosidad(corte);
        BigDecimal provisiones = CalculosCartera.provision(corte.operaciones().stream()
                .filter(operacion -> operacion.estadoCartera() != EstadoCartera.CASTIGADA).toList());
        return CalculosCartera.porcentaje(provisiones,
                morosidad.saldoVencido().add(morosidad.saldoEnLitigio()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoRollRate consultarRollRate(LocalDate fechaInicial, LocalDate fechaFinal) {
        validarFecha(fechaInicial);
        validarFecha(fechaFinal);
        if (!fechaInicial.isBefore(fechaFinal)) {
            throw new IllegalArgumentException("La fecha inicial debe ser anterior a la fecha final.");
        }
        CorteCartera inicial = consultarCorte(fechaInicial);
        validarConfirmacion(inicial);
        CorteCartera fin = consultarCorte(fechaFinal);
        validarConfirmacion(fin);
        return calcularRollRate(inicial, fin);
    }

    private void validarConfirmacion(CorteCartera corte) {
        String mensaje = "Fotografía no confirmada para " + corte.fechaCorte() + ". ";
        if (corte.operaciones().isEmpty()) {
            throw new IllegalStateException(mensaje + "Un corte vacío no permite acreditar su generación.");
        }
        ResultadoCierreDia cierre;
        try {
            cierre = servicioCierre.consultarCierre(corte.fechaCorte());
        } catch (RuntimeException e) {
            throw new IllegalStateException(mensaje + "No se pudo verificar el cierre correspondiente.", e);
        }
        if (cierre == null || !corte.fechaCorte().equals(cierre.fechaNegocio())
                || !"COMPLETADO".equals(cierre.estadoCierre()) || cierre.ejecuciones() == null
                || cierre.ejecuciones().stream().anyMatch(ejecucion -> ejecucion == null)) {
            throw new IllegalStateException(mensaje + "El cierre no está completado o sus datos son inválidos.");
        }
        var intentos = cierre.ejecuciones().stream()
                .filter(ejecucion -> ejecucion.etapa() == PasoCierre.FOTOGRAFIA).toList();
        int ultimo = intentos.stream().mapToInt(ejecucion -> ejecucion.numeroIntento()).max().orElse(0);
        var finales = intentos.stream().filter(ejecucion -> ejecucion.numeroIntento() == ultimo).toList();
        if (ultimo <= 0 || finales.size() != 1) {
            throw new IllegalStateException(mensaje + "No existe un último intento de fotografía inequívoco.");
        }
        var ejecucion = finales.getFirst();
        if (ejecucion.numeroEtapa() != PasoCierre.FOTOGRAFIA.numeroEtapa()
                || !"EXITOSA".equals(ejecucion.estadoEtapa())
                || ejecucion.fechaHoraInicio() == null || ejecucion.fechaHoraFin() == null
                || ejecucion.fechaHoraFin().isBefore(ejecucion.fechaHoraInicio())
                || ejecucion.registrosProcesados() <= 0
                || ejecucion.registrosProcesados() != corte.operaciones().size()) {
            throw new IllegalStateException(mensaje + "La ejecución y los registros almacenados no acreditan un corte consistente.");
        }
    }

    private CorteCartera consultarCorte(LocalDate fecha) {
        validarFecha(fecha);
        var fotos = servicioFotografia.consultarFotosPorFecha(fecha);
        var tramos = servicioFotografia.consultarTramosVigentes(fecha);
        if (fotos == null || tramos == null || tramos.isEmpty()) {
            throw new IllegalArgumentException("La fuente no proporciona fotografías o configuración de tramos válida.");
        }
        List<CorteCartera.Tramo> configuracion = tramos.stream().map(tramo -> {
            if (tramo == null || tramo.fechaVigenciaDesde() == null
                    || tramo.fechaVigenciaDesde().isAfter(fecha)
                    || (tramo.fechaVigenciaHasta() != null && !fecha.isBefore(tramo.fechaVigenciaHasta()))) {
                throw new IllegalArgumentException("El tramo no está vigente para la fecha consultada.");
            }
            return new CorteCartera.Tramo(tramo.idTramo(), tramo.numeroTramo(), tramo.diasMoraDesde(),
                    tramo.diasMoraHasta(), estado(tramo.estadoCartera()), tramo.porcentajeProvision());
        }).toList();
        List<CorteCartera.Operacion> operaciones = fotos.stream().map(foto -> adaptarFoto(foto, fecha)).toList();
        CorteCartera corte = new CorteCartera(fecha, operaciones, configuracion);
        CalculosCartera.validar(corte);
        return corte;
    }

    private CorteCartera.Operacion adaptarFoto(ResumenFotoCartera foto, LocalDate fecha) {
        if (foto == null || !fecha.equals(foto.fechaFoto()) || foto.diasMora() < 0) {
            throw new IllegalArgumentException("La fotografía es inválida o pertenece a otra fecha.");
        }
        return new CorteCartera.Operacion(foto.idOperacion(), foto.idTramo(), estado(foto.estadoCartera()),
                foto.saldoCapital(), foto.valorProvision());
    }

    private EstadoCartera estado(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("El estado de cartera es obligatorio.");
        }
        try {
            return EstadoCartera.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de cartera desconocido: " + valor, e);
        }
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de corte es obligatoria.");
        }
    }


    @Override
    public ResultadoMorosidad calcularMorosidad(CorteCartera corte) {
        CalculosCartera.validar(corte);
        Map<EstadoCartera, BigDecimal> saldos = new EnumMap<>(EstadoCartera.class);
        for (CorteCartera.Operacion operacion : corte.operaciones()) {
            saldos.merge(operacion.estadoCartera(), operacion.saldoCapital(), BigDecimal::add);
        }
        BigDecimal porVencer = saldos.getOrDefault(EstadoCartera.POR_VENCER, CalculosCartera.CERO);
        BigDecimal vencida = saldos.getOrDefault(EstadoCartera.VENCIDA, CalculosCartera.CERO);
        BigDecimal litigio = saldos.getOrDefault(EstadoCartera.EN_LITIGIO, CalculosCartera.CERO);
        BigDecimal castigada = saldos.getOrDefault(EstadoCartera.CASTIGADA, CalculosCartera.CERO);
        BigDecimal bruta = porVencer.add(vencida).add(litigio);
        return new ResultadoMorosidad(corte.fechaCorte(), porVencer, vencida, litigio, bruta, castigada,
                CalculosCartera.porcentaje(vencida.add(litigio), bruta));
    }

    @Override
    public ResultadoComposicionCartera calcularComposicion(CorteCartera corte) {
        CalculosCartera.validar(corte);
        List<CorteCartera.Tramo> tramos = CalculosCartera.tramosOrdenados(corte);
        Map<Integer, List<CorteCartera.Operacion>> porTramo = corte.operaciones().stream()
                .collect(Collectors.groupingBy(CorteCartera.Operacion::idTramo));
        List<BigDecimal> saldos = tramos.stream().map(tramo ->
                CalculosCartera.saldo(porTramo.getOrDefault(tramo.idTramo(), List.of()))).toList();
        List<BigDecimal> participaciones = CalculosCartera.participaciones(saldos);
        List<ResultadoComposicionCartera.DetalleTramo> detalles = new ArrayList<>();
        for (int i = 0; i < tramos.size(); i++) {
            CorteCartera.Tramo tramo = tramos.get(i);
            List<CorteCartera.Operacion> operaciones = porTramo.getOrDefault(tramo.idTramo(), List.of());
            detalles.add(new ResultadoComposicionCartera.DetalleTramo(tramo, operaciones.size(), saldos.get(i),
                    participaciones.get(i), CalculosCartera.provision(operaciones)));
        }
        return new ResultadoComposicionCartera(corte.fechaCorte(), detalles, corte.operaciones().size(),
                CalculosCartera.saldo(corte.operaciones()), CalculosCartera.provision(corte.operaciones()));
    }

    @Override
    public ResultadoRollRate calcularRollRate(CorteCartera corteInicial, CorteCartera corteFinal) {
        return new CalculadoraRollRate().calcular(corteInicial, corteFinal);
    }

    @Override
    public ResultadoConcentracion calcularConcentracion(CorteCartera corte, List<DatosOperacionReporte> datosOperaciones) {
        return new CalculadoraConcentracion().calcular(corte, datosOperaciones);
    }
    @Override
    public ResultadoSaludCartera calcularSalud(CorteCartera corte, List<DatosOperacionReporte> datosOperaciones) {
        return new CalculadoraSaludCartera().calcular(corte, datosOperaciones,
                calcularMorosidad(corte), calcularConcentracion(corte, datosOperaciones));
    }
}
