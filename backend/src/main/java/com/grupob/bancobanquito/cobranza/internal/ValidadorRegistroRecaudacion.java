package com.grupob.bancobanquito.cobranza.internal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ValidadorRegistroRecaudacion {

    private static final Set<String> CANALES = Set.of("VEN", "WEB", "DEB", "TRF");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    RegistroRecaudacionLeido validar(String linea, int posicion) {
        try {
            List<String> campos = CamposCsv.leer(linea);
            if (!formatoValido(campos, linea, posicion)) {
                return rechazado(linea, posicion, "REG-001");
            }
            UUID uuid = UUID.fromString(campos.get(2));
            LocalDateTime fechaCobro = LocalDateTime.parse(campos.get(6), FECHA_HORA);
            String montoTexto = campos.get(5);
            if (!montoTexto.matches("-?[0-9]+(?:\\.[0-9]+)?")) {
                return rechazado(linea, posicion, "REG-001");
            }
            BigDecimal monto = new BigDecimal(montoTexto);
            String codigo = null;
            if (monto.signum() <= 0 || monto.scale() > 2) {
                codigo = "REG-005";
            } else if (!montoTexto.matches("[0-9]+\\.[0-9]{2}")
                    || monto.compareTo(new BigDecimal("99999999.99")) > 0) {
                codigo = "REG-001";
            }
            return new RegistroRecaudacionLeido(posicion, linea, uuid, campos.get(3), campos.get(4),
                    monto, fechaCobro, campos.get(7), campos.get(8), codigo);
        } catch (IllegalArgumentException | java.time.DateTimeException excepcion) {
            return rechazado(linea, posicion, "REG-001");
        }
    }

    private boolean formatoValido(List<String> campos, String linea, int posicion) {
        return campos.size() == 9 && campos.get(0).equals("D") && linea.length() <= 500
                && campos.get(1).matches("[1-9][0-9]*")
                && Integer.parseInt(campos.get(1)) == posicion
                && campos.get(2).matches("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
                && !campos.get(3).isBlank() && campos.get(3).length() <= 20
                && campos.get(4).matches("[0-9]{10}")
                && campos.get(6).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}")
                && CANALES.contains(campos.get(7)) && campos.get(8).length() <= 100;
    }

    private RegistroRecaudacionLeido rechazado(String linea, int posicion, String codigo) {
        return new RegistroRecaudacionLeido(posicion, linea, null, null, null, null, null, null, null, codigo);
    }
}
