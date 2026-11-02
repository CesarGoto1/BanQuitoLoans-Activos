package com.grupob.bancobanquito.reportes.internal;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.reportes.CorteCartera;
import com.grupob.bancobanquito.reportes.ResultadoSaludCartera;
import com.grupob.bancobanquito.reportes.DatosOperacionReporte;
import com.grupob.bancobanquito.reportes.ResultadoConcentracion;
import com.grupob.bancobanquito.reportes.ResultadoComposicionCartera;
import com.grupob.bancobanquito.reportes.ResultadoMorosidad;
import com.grupob.bancobanquito.reportes.ResultadoRollRate;
import com.grupob.bancobanquito.reportes.ServiceReportesCartera;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class ServiceReportesCarteraImpl implements ServiceReportesCartera {

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
