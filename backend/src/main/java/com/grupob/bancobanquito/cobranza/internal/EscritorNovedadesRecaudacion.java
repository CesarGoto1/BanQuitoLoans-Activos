package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cobranza.ResultadoRecaudacion;
import com.grupob.bancobanquito.cobranza.ResultadoRegistroRecaudacion;
import com.grupob.bancobanquito.pagos.ResultadoPago;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
class EscritorNovedadesRecaudacion {

    private static final Map<String, String> DESCRIPCIONES = Map.of(
            "REG-001", "Formato invalido",
            "REG-002", "Transaccion duplicada",
            "REG-003", "La operacion no existe",
            "REG-004", "La identificacion no corresponde al titular",
            "REG-005", "Monto invalido",
            "REG-006", "La operacion esta cancelada",
            "REG-007", "Sobrepago: el excedente se devuelve");

    void escribir(Path destino, ResultadoRecaudacion resultado, LocalDate fechaProceso) throws IOException {
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.writeString(temporal, contenido(resultado, fechaProceso), StandardCharsets.UTF_8);
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    String contenido(ResultadoRecaudacion resultado, LocalDate fechaProceso) {
        String idArchivo = resultado.nombreArchivo().endsWith(".csv")
                ? resultado.nombreArchivo().substring(0, resultado.nombreArchivo().length() - 4)
                : resultado.nombreArchivo();
        StringBuilder salida = new StringBuilder();
        salida.append(String.join(",", "H", idArchivo, fechaProceso.toString(), resultado.estado(),
                texto(resultado.codigoRechazo()))).append('\n');
        if (!"PROCESADO".equals(resultado.estado())) {
            return salida.toString();
        }
        List<ResultadoRegistroRecaudacion> registros = resultado.registros().stream()
                .sorted(Comparator.comparingInt(ResultadoRegistroRecaudacion::posicion))
                .toList();
        int aplicados = 0;
        int rechazados = 0;
        int duplicados = 0;
        BigDecimal montoTotal = BigDecimal.ZERO;
        BigDecimal montoAplicado = BigDecimal.ZERO;
        for (ResultadoRegistroRecaudacion registro : registros) {
            BigDecimal recibido = Objects.requireNonNullElse(registro.montoRecibido(), BigDecimal.ZERO);
            montoTotal = montoTotal.add(recibido);
            ResultadoPago pago = "APLICADO".equals(registro.estado()) ? registro.pago() : null;
            BigDecimal excedente = BigDecimal.ZERO;
            if (pago != null) {
                aplicados++;
                excedente = cero(pago.excedenteDevolver());
                montoAplicado = montoAplicado.add(recibido.subtract(excedente));
            } else if ("DUPLICADO".equals(registro.estado())) {
                duplicados++;
            } else {
                rechazados++;
                excedente = recibido;
            }
            salida.append(String.join(",", "D",
                    String.valueOf(registro.posicion()),
                    registro.uuidTransaccion() == null ? "" : registro.uuidTransaccion().toString(),
                    texto(registro.numeroOperacion()),
                    registro.estado(),
                    texto(registro.codigoRechazo()),
                    descripcion(registro.codigoRechazo()),
                    monto(recibido),
                    monto(pago == null ? null : pago.aplicadoSeguro()),
                    monto(pago == null ? null : pago.aplicadoMora()),
                    monto(pago == null ? null : pago.aplicadoInteres()),
                    monto(pago == null ? null : pago.aplicadoCapital()),
                    monto(pago == null ? null : pago.aplicadoAnticipado()),
                    monto(excedente))).append('\n');
        }
        salida.append(String.join(",", "T",
                String.valueOf(registros.size()),
                String.valueOf(aplicados),
                String.valueOf(rechazados),
                String.valueOf(duplicados),
                monto(montoAplicado),
                monto(montoTotal.subtract(montoAplicado)))).append('\n');
        return salida.toString();
    }

    private String descripcion(String codigo) {
        return codigo == null ? "Pago aplicado" : DESCRIPCIONES.getOrDefault(codigo, codigo);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private BigDecimal cero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private String monto(BigDecimal valor) {
        BigDecimal numero = cero(valor);
        return numero.scale() <= 2 ? numero.setScale(2, RoundingMode.UNNECESSARY).toPlainString() : numero.toPlainString();
    }
}
