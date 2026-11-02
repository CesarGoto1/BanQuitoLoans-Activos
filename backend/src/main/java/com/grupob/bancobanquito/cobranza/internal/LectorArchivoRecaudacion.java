package com.grupob.bancobanquito.cobranza.internal;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
class LectorArchivoRecaudacion {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private final ValidadorRegistroRecaudacion validadorRegistro;

    LectorArchivoRecaudacion(ValidadorRegistroRecaudacion validadorRegistro) {
        this.validadorRegistro = validadorRegistro;
    }

    ArchivoRecaudacionLeido leer(Path archivo, LocalDate fechaNegocio) throws IOException {
        return leer(archivo.getFileName().toString(), Files.readAllBytes(archivo), fechaNegocio);
    }

    ArchivoRecaudacionLeido leer(String nombre, byte[] contenido, LocalDate fechaNegocio) {
        Objects.requireNonNull(fechaNegocio, "La fecha de negocio es obligatoria.");
        LocalDate fechaNombre = fechaDelNombre(nombre);
        List<String> lineas = lineas(contenido);
        List<String> cabecera = control(lineas.getFirst(), "H", 6);
        List<String> pie = control(lineas.getLast(), "T", 4);
        List<String> detalles = lineas.subList(1, lineas.size() - 1);
        if (!cabecera.get(1).equals(nombre.substring(0, nombre.length() - 4))
                || detalles.stream().anyMatch(linea -> !linea.startsWith("D,"))) {
            throw error("ARC-002", "Identificador de archivo o estructura de detalles inválidos.");
        }
        LocalDate fechaProceso;
        LocalDateTime fechaGeneracion;
        try {
            if (!cabecera.get(2).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                    || !cabecera.get(3).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}")) {
                throw new IllegalArgumentException();
            }
            fechaProceso = LocalDate.parse(cabecera.get(2));
            fechaGeneracion = LocalDateTime.parse(cabecera.get(3), FECHA_HORA);
        } catch (IllegalArgumentException | java.time.DateTimeException excepcion) {
            throw error("ARC-002", "Fechas de cabecera inválidas.");
        }
        if (!fechaNombre.equals(fechaProceso)) {
            throw error("ARC-002", "La fecha del nombre no coincide con la cabecera.");
        }
        if (!fechaProceso.equals(fechaNegocio)) {
            throw error("ARC-007", "El archivo no corresponde a la fecha de negocio.");
        }
        if (cantidad(cabecera.get(4)) != detalles.size() || cantidad(pie.get(1)) != detalles.size()) {
            throw error("ARC-003", "La cantidad de registros no coincide.");
        }
        String hash = calcularHash(detalles);
        if (!pie.get(3).matches("[0-9a-fA-F]{64}") || !hash.equalsIgnoreCase(pie.get(3))) {
            throw error("ARC-005", "El hash de los detalles no coincide.");
        }
        BigDecimal total = montoControl(cabecera.get(5));
        if (total.compareTo(montoControl(pie.get(2))) != 0 || total.compareTo(sumarMontos(detalles)) != 0) {
            throw error("ARC-004", "Los montos de control no coinciden con los detalles.");
        }
        List<RegistroRecaudacionLeido> registros = new ArrayList<>();
        for (int indice = 0; indice < detalles.size(); indice++) {
            registros.add(validadorRegistro.validar(detalles.get(indice), indice + 1));
        }
        return new ArchivoRecaudacionLeido(nombre, hash, fechaProceso, fechaGeneracion, total, registros);
    }

    private LocalDate fechaDelNombre(String nombre) {
        if (nombre == null || !nombre.matches("REC_[0-9]{8}_[0-9]{3}\\.csv")) {
            throw error("ARC-001", "Nombre de archivo inválido.");
        }
        try {
            return LocalDate.parse(nombre.substring(4, 12), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (java.time.DateTimeException excepcion) {
            throw error("ARC-001", "Fecha inválida en el nombre del archivo.");
        }
    }

    private List<String> lineas(byte[] contenido) {
        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(contenido)).toString();
        } catch (CharacterCodingException excepcion) {
            throw error("ARC-002", "El archivo debe estar codificado en UTF-8.");
        }
        if (texto.contains("\r") || texto.indexOf('\0') >= 0) {
            throw error("ARC-002", "El archivo debe utilizar líneas LF sin caracteres nulos.");
        }
        if (texto.endsWith("\n")) {
            texto = texto.substring(0, texto.length() - 1);
        }
        List<String> lineas = Arrays.asList(texto.split("\n", -1));
        if (lineas.size() < 2 || lineas.stream().anyMatch(String::isBlank)) {
            throw error("ARC-002", "Faltan registros de control o existen líneas vacías.");
        }
        return lineas;
    }

    private List<String> control(String linea, String tipo, int cantidadCampos) {
        try {
            List<String> campos = CamposCsv.leer(linea);
            if (campos.size() == cantidadCampos && campos.getFirst().equals(tipo)) {
                return campos;
            }
        } catch (IllegalArgumentException excepcion) {
            throw error("ARC-002", "Registro de control CSV inválido.");
        }
        throw error("ARC-002", "Cabecera o pie inválidos.");
    }

    private int cantidad(String texto) {
        try {
            if (texto.matches("[0-9]+")) {
                return Integer.parseInt(texto);
            }
        } catch (NumberFormatException excepcion) {
            throw error("ARC-003", "Cantidad de registros fuera de rango.");
        }
        throw error("ARC-003", "Cantidad de registros inválida.");
    }

    private BigDecimal montoControl(String texto) {
        if (!texto.matches("[0-9]+\\.[0-9]{2}")) {
            throw error("ARC-004", "Monto de control inválido.");
        }
        BigDecimal monto = new BigDecimal(texto);
        if (monto.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw error("ARC-004", "Monto de control fuera de rango.");
        }
        return monto;
    }

    private BigDecimal sumarMontos(List<String> detalles) {
        BigDecimal total = BigDecimal.ZERO;
        for (String detalle : detalles) {
            try {
                List<String> campos = CamposCsv.leerPrimeros(detalle, 6);
                if (campos.size() <= 5 || !campos.get(5).matches("-?[0-9]+(?:\\.[0-9]+)?")) {
                    throw new IllegalArgumentException();
                }
                total = total.add(new BigDecimal(campos.get(5)));
            } catch (IllegalArgumentException excepcion) {
                throw error("ARC-004", "No se puede verificar el total por un monto de detalle ilegible.");
            }
        }
        return total;
    }

    private String calcularHash(List<String> detalles) {
        try {
            byte[] bytes = String.join("\n", detalles).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException excepcion) {
            throw new IllegalStateException("SHA-256 no está disponible.", excepcion);
        }
    }

    private ArchivoRecaudacionInvalidoException error(String codigo, String mensaje) {
        return new ArchivoRecaudacionInvalidoException(codigo, mensaje);
    }
}
