package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cierre.EtapaCierreException;
import com.grupob.bancobanquito.cobranza.ResultadoRecaudacion;
import com.grupob.bancobanquito.cobranza.ResultadoRegistroRecaudacion;
import com.grupob.bancobanquito.cobranza.ServiceRecaudaciones;
import com.grupob.bancobanquito.pagos.EstadoAplicacionPago;
import com.grupob.bancobanquito.pagos.PagoRecaudacion;
import com.grupob.bancobanquito.pagos.ResultadoPago;
import com.grupob.bancobanquito.pagos.ServicePagos;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class ServiceRecaudacionesImpl implements ServiceRecaudaciones {

    private static final Set<String> INDICES_IDEMPOTENCIA = Set.of("uqArchivoNombreHash", "uqRegistroUuidAplicado");
    private final LectorArchivoRecaudacion lector;
    private final ArchivoRecaudacionRepository archivos;
    private final RegistroRecaudacionRepository registros;
    private final ServiceFechaNegocio fechaNegocio;
    private final ObjectProvider<ServicePagos> pagos;
    private final TransactionTemplate transaccion;
    private final EscritorNovedadesRecaudacion escritorNovedades;
    private final Path carpetaEntrada;
    private final Path carpetaNovedades;

    ServiceRecaudacionesImpl(LectorArchivoRecaudacion lector, ArchivoRecaudacionRepository archivos,
                             RegistroRecaudacionRepository registros, ServiceFechaNegocio fechaNegocio,
                             ObjectProvider<ServicePagos> pagos, PlatformTransactionManager transacciones,
                             EscritorNovedadesRecaudacion escritorNovedades,
                             @Value("${recaudacion.carpeta-entrada:./recaudaciones}") String carpetaEntrada,
                             @Value("${recaudacion.carpeta-novedades:./novedades}") String carpetaNovedades) {
        this.escritorNovedades = escritorNovedades;
        this.carpetaEntrada = Path.of(carpetaEntrada);
        this.carpetaNovedades = Path.of(carpetaNovedades);
        this.lector = lector;
        this.archivos = archivos;
        this.registros = registros;
        this.fechaNegocio = fechaNegocio;
        this.pagos = pagos;
        transaccion = new TransactionTemplate(transacciones);
        transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaccion.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public ResultadoRecaudacion procesar(String nombreArchivo, byte[] contenido) {
        if (nombreArchivo == null || nombreArchivo.isBlank() || nombreArchivo.length() > 30) {
            throw new IllegalArgumentException("El nombre recibido debe tener entre 1 y 30 caracteres para registrar el intento.");
        }
        Objects.requireNonNull(contenido, "El contenido es obligatorio.");
        LocalDate fecha = Objects.requireNonNull(fechaNegocio.getCurrentBusinessDate());
        ArchivoRecaudacionLeido entrada;
        try {
            entrada = lector.leer(nombreArchivo, contenido, fecha);
        } catch (ArchivoRecaudacionInvalidoException excepcion) {
            return transaccion.execute(estado -> rechazarArchivo(nombreArchivo, fecha, excepcion.codigo()));
        }
        for (int intento = 0; intento < 3; intento++) {
            try {
                return transaccion.execute(estado -> procesarArchivo(entrada, fecha));
            } catch (DataIntegrityViolationException excepcion) {
                if (intento == 2 || !conflictoIdempotencia(excepcion)) {
                    throw excepcion;
                }
            }
        }
        throw new IllegalStateException("No fue posible completar el procesamiento.");
    }

    @Override
    public int procesarCarpeta(LocalDate fechaNegocioCierre) {
        if (!Files.isDirectory(carpetaEntrada)) {
            return 0;
        }
        String prefijo = "REC_" + fechaNegocioCierre.format(DateTimeFormatter.BASIC_ISO_DATE) + "_";
        int procesados = 0;
        try {
            Files.createDirectories(carpetaNovedades);
            for (Path archivo : archivosDelDia(prefijo)) {
                String nombre = archivo.getFileName().toString();
                Path novedades = carpetaNovedades.resolve("NOV_" + nombre);
                if (Files.exists(novedades)) {
                    continue;
                }
                ResultadoRecaudacion resultado = procesar(nombre, Files.readAllBytes(archivo));
                escritorNovedades.escribir(novedades, resultado, fechaNegocioCierre);
                procesados++;
            }
        } catch (IOException excepcion) {
            throw new EtapaCierreException("No se pudo leer o escribir los archivos de recaudación: "
                    + excepcion.getMessage());
        }
        return procesados;
    }

    private List<Path> archivosDelDia(String prefijo) throws IOException {
        try (Stream<Path> contenido = Files.list(carpetaEntrada)) {
            return contenido.filter(Files::isRegularFile)
                    .filter(archivo -> {
                        String nombre = archivo.getFileName().toString();
                        return nombre.startsWith(prefijo) && nombre.length() <= 30;
                    })
                    .sorted()
                    .toList();
        }
    }

    private ResultadoRecaudacion procesarArchivo(ArchivoRecaudacionLeido entrada, LocalDate fecha) {
        if (archivos.existsByNombreArchivoAndHashSha256AndEstadoArchivo(entrada.nombre(), entrada.hashSha256(), "PROCESADO")) {
            return rechazarArchivo(entrada.nombre(), fecha, "ARC-006");
        }
        ArchivoRecaudacion archivo = archivos.saveAndFlush(ArchivoRecaudacion.procesado(entrada, fecha));
        List<ResultadoRegistroRecaudacion> resultados = new ArrayList<>();
        for (RegistroRecaudacionLeido registro : entrada.registros()) {
            resultados.add(procesarRegistro(archivo.id(), registro, fecha));
        }
        return new ResultadoRecaudacion(archivo.id(), entrada.nombre(), "PROCESADO", null, resultados);
    }

    private ResultadoRegistroRecaudacion procesarRegistro(Integer idArchivo, RegistroRecaudacionLeido entrada,
                                                          LocalDate fecha) {
        if (entrada.codigoRechazo() != null) {
            return registrarResultado(idArchivo, entrada, "RECHAZADO", entrada.codigoRechazo());
        }
        if (registros.existsByUuidTransaccionAndEstadoRegistro(entrada.uuidTransaccion(), "APLICADO")) {
            return registrarResultado(idArchivo, entrada, "DUPLICADO", "REG-002");
        }
        ServicePagos servicioPagos = pagos.getIfAvailable();
        if (servicioPagos == null) {
            throw new IllegalStateException("El servicio de pagos todavía no está disponible. No se aplicó el archivo.");
        }
        RegistroRecaudacion registro = registros.saveAndFlush(new RegistroRecaudacion(idArchivo, entrada, "APLICADO", null));
        ResultadoPago resultado = servicioPagos.aplicarPagoRecaudacion(new PagoRecaudacion(registro.id(),
                entrada.uuidTransaccion(), entrada.numeroOperacion(), entrada.identificacion(), entrada.monto(),
                entrada.fechaCobro(), entrada.canal(), fecha));
        validarResultado(resultado);
        String estado = resultado.estado().name();
        registro.finalizar(estado, resultado.codigoRechazo());
        registros.flush();
        return new ResultadoRegistroRecaudacion(registro.id(), entrada.posicion(), entrada.uuidTransaccion(),
                entrada.numeroOperacion(), entrada.monto(), estado, resultado.codigoRechazo(), resultado);
    }

    private ResultadoRegistroRecaudacion registrarResultado(Integer idArchivo, RegistroRecaudacionLeido entrada,
                                                            String estado, String codigo) {
        RegistroRecaudacion registro = registros.saveAndFlush(new RegistroRecaudacion(idArchivo, entrada, estado, codigo));
        return new ResultadoRegistroRecaudacion(registro.id(), entrada.posicion(), entrada.uuidTransaccion(),
                entrada.numeroOperacion(), entrada.monto(), estado, codigo, null);
    }

    private ResultadoRecaudacion rechazarArchivo(String nombre, LocalDate fecha, String codigo) {
        ArchivoRecaudacion archivo = archivos.saveAndFlush(ArchivoRecaudacion.rechazado(nombre, fecha, codigo));
        return new ResultadoRecaudacion(archivo.id(), nombre, "RECHAZADO", codigo, List.of());
    }

    private void validarResultado(ResultadoPago resultado) {
        if (resultado == null || resultado.estado() == null) {
            throw new IllegalStateException("El servicio de pagos devolvió un resultado incompleto.");
        }
        String codigo = resultado.codigoRechazo();
        boolean valido = resultado.estado() == EstadoAplicacionPago.APLICADO
                ? codigo == null || codigo.equals("REG-007")
                : Set.of("REG-001", "REG-003", "REG-004", "REG-005", "REG-006").contains(codigo == null ? "" : codigo);
        if (!valido) {
            throw new IllegalStateException("El resultado de pagos no es compatible con los estados de recaudación.");
        }
    }

    private boolean conflictoIdempotencia(Throwable excepcion) {
        for (Throwable causa = excepcion; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException restriccion
                    && "23505".equals(restriccion.getSQLState())
                    && restriccion.getConstraintName() != null
                    && INDICES_IDEMPOTENCIA.contains(restriccion.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
