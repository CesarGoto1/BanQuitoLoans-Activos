package com.grupob.bancobanquito.cobranza.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LectorArchivoRecaudacionTest {

    private static final String NOMBRE = "REC_20261031_001.csv";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);
    private static final String DETALLE = "D,1,3f6c2a1e-8b4d-4c7a-9e2f-1a2b3c4d5e01,OP-20261031-000001,1710034065,10.00,2026-10-31T09:15:00,VEN,";
    private final LectorArchivoRecaudacion lector = new LectorArchivoRecaudacion(new ValidadorRegistroRecaudacion());

    @Test
    void leeElEjemploDelEquipoConSuHashPublicado() throws Exception {
        byte[] contenido;
        try (var entrada = getClass().getResourceAsStream("/recaudaciones/REC_20261031_001.csv")) {
            assertThat(entrada).isNotNull();
            contenido = entrada.readAllBytes();
        }
        String texto = new String(contenido, StandardCharsets.UTF_8).replace("\r\n", "\n");
        ArchivoRecaudacionLeido archivo = leer(texto);
        assertThat(archivo.hashSha256()).isEqualTo("0684a720715dc6718b24732b73ce16522a473af2ebf368af0e0049b6cec98377");
        assertThat(archivo.montoTotal()).isEqualByComparingTo("2346.15");
        assertThat(archivo.registros()).hasSize(3).allSatisfy(registro -> assertThat(registro.codigoRechazo()).isNull());
    }

    @Test
    void leeDesdeUnaRutaSinModificarElArchivo(@TempDir Path carpeta) throws Exception {
        Path ruta = carpeta.resolve(NOMBRE);
        String contenido = archivo(List.of(DETALLE), "10.00");
        Files.writeString(ruta, contenido);
        assertThat(lector.leer(ruta, FECHA).registros()).hasSize(1);
        assertThat(Files.readString(ruta)).isEqualTo(contenido);
    }

    @ParameterizedTest
    @ValueSource(strings = {"otro.csv", "REC_20260230_001.csv", "../REC_20261031_001.csv", "REC_20261031_01.csv"})
    void rechazaNombresInvalidos(String nombre) {
        assertThatThrownBy(() -> lector.leer(nombre, new byte[0], FECHA))
                .isInstanceOfSatisfying(ArchivoRecaudacionInvalidoException.class,
                        error -> assertThat(error.codigo()).isEqualTo("ARC-001"));
    }

    @Test
    void rechazaUtf8Malformado() {
        assertThatThrownBy(() -> lector.leer(NOMBRE, new byte[]{(byte) 0xc3, 0x28}, FECHA))
                .isInstanceOfSatisfying(ArchivoRecaudacionInvalidoException.class,
                        error -> assertThat(error.codigo()).isEqualTo("ARC-002"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\n", "H,incompleta\nT,incompleto", "D,1\nT,0,0.00,x"})
    void rechazaEstructurasIncompletas(String contenido) {
        verificarRechazo(contenido, "ARC-002");
    }

    @ParameterizedTest
    @CsvSource({"cantidad,ARC-003", "monto,ARC-004", "hash,ARC-005", "fecha,ARC-007", "crlf,ARC-002", "generacion,ARC-002", "lineaVacia,ARC-002"})
    void rechazaControlesInconsistentes(String cambio, String codigo) throws Exception {
        String contenido = archivo(List.of(DETALLE), "10.00");
        contenido = switch (cambio) {
            case "cantidad" -> contenido.replace(",1,10.00", ",2,10.00");
            case "monto" -> contenido.replace(",1,10.00", ",1,20.00");
            case "hash" -> contenido.replace("10.00,2026-10-31T09", "11.00,2026-10-31T09");
            case "fecha" -> contenido.replace("2026-10-31", "2026-10-30").replace("20261031", "20261030");
            case "crlf" -> contenido.replace("\n", "\r\n");
            case "generacion" -> contenido.replace("18:00:00", "25:00:00");
            case "lineaVacia" -> contenido + "\n\n";
            default -> throw new IllegalArgumentException(cambio);
        };
        if (cambio.equals("fecha")) {
            String entrada = contenido;
            assertThatThrownBy(() -> lector.leer("REC_20261030_001.csv", entrada.getBytes(StandardCharsets.UTF_8), FECHA))
                    .isInstanceOfSatisfying(ArchivoRecaudacionInvalidoException.class,
                            error -> assertThat(error.codigo()).isEqualTo(codigo));
        } else {
            verificarRechazo(contenido, codigo);
        }
    }

    @Test
    void continuaDespuesDeUnRegistroInvalido() throws Exception {
        String invalido = DETALLE.replace(",VEN,", ",OTRO,");
        String valido = DETALLE.replace("D,1,", "D,2,");
        var registros = leer(archivo(List.of(invalido, valido), "20.00")).registros();
        assertThat(registros.getFirst().codigoRechazo()).isEqualTo("REG-001");
        assertThat(registros.getLast().codigoRechazo()).isNull();
        assertThat(registros.getFirst().lineaOriginal()).isEqualTo(invalido);
    }

    @Test
    void unaReferenciaMalformadaNoImpideVerificarLosMontos() throws Exception {
        String detalle = DETALLE + "\"referencia sin cerrar";
        assertThat(leer(archivo(List.of(detalle), "10.00")).registros().getFirst().codigoRechazo()).isEqualTo("REG-001");
    }

    @Test
    void rechazaElArchivoSiNoSePuedeVerificarElTotal() throws Exception {
        verificarRechazo(archivo(List.of(DETALLE.replace(",10.00,", ",texto,")), "10.00"), "ARC-004");
    }

    @Test
    void conservaLosDecimalesInvalidosSinRedondearlos() throws Exception {
        String primero = DETALLE.replace(",10.00,", ",0.001,");
        String segundo = DETALLE.replace("D,1,", "D,2,").replace(",10.00,", ",0.009,");
        var registros = leer(archivo(List.of(primero, segundo), "0.01")).registros();
        assertThat(registros).allSatisfy(registro -> assertThat(registro.codigoRechazo()).isEqualTo("REG-005"));
        assertThat(registros.getFirst().monto()).isEqualByComparingTo("0.001");
    }

    @Test
    void admiteUnArchivoVacioYUnLfFinalOpcional() throws Exception {
        String contenido = archivo(List.of(), "0.00");
        assertThat(leer(contenido).registros()).isEmpty();
        assertThat(leer(contenido + "\n").registros()).isEmpty();
    }

    private ArchivoRecaudacionLeido leer(String contenido) {
        return lector.leer(NOMBRE, contenido.getBytes(StandardCharsets.UTF_8), FECHA);
    }

    private void verificarRechazo(String contenido, String codigo) {
        assertThatThrownBy(() -> leer(contenido)).isInstanceOfSatisfying(ArchivoRecaudacionInvalidoException.class,
                error -> assertThat(error.codigo()).isEqualTo(codigo));
    }

    private String archivo(List<String> detalles, String total) throws Exception {
        String cuerpo = String.join("\n", detalles);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cuerpo.getBytes(StandardCharsets.UTF_8)));
        return "H,REC_20261031_001,2026-10-31,2026-10-31T18:00:00," + detalles.size() + "," + total + "\n"
                + (cuerpo.isEmpty() ? "" : cuerpo + "\n") + "T," + detalles.size() + "," + total + "," + hash;
    }
}
