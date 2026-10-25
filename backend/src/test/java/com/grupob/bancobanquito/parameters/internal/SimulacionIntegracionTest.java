package com.grupob.bancobanquito.parameters.internal;

import com.grupob.bancobanquito.parameters.ParameterService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import com.grupob.bancobanquito.parameters.ParameterNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:simulacion;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SimulacionIntegracionTest {
    private final EntityManager entidades;
    private final TransactionTemplate transacciones;
    private final ParameterRepository repositorio;
    private final ParameterService parametros;
    private final Environment entorno;
    private final LocalDate fecha = LocalDate.of(2026, 10, 31);
    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    SimulacionIntegracionTest(EntityManager entidades, TransactionTemplate transacciones,
            ParameterRepository repositorio, ParameterService parametros, Environment entorno) {
        this.entidades = entidades;
        this.transacciones = transacciones;
        this.repositorio = repositorio;
        this.parametros = parametros;
        this.entorno = entorno;
    }

    @BeforeEach
    void cargarDatosAislados() {
        transacciones.executeWithoutResult(estado -> ParameterTestData.load(entidades, repositorio));
    }

    private void guardarParametro(String codigo, String valor, LocalDate desde, LocalDate hasta) {
        ParameterTestData.save(entidades, codigo, valor, desde, hasta);
    }

    private HttpResponse<String> solicitar(String ruta, String cuerpo) throws Exception {
        var peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + entorno.getProperty("local.server.port") + ruta))
                .header("X-Identificacion", "1700000000").header("X-Rol", "CLIENTE");
        if (cuerpo != null) {
            peticion.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(cuerpo));
        }
        return HttpClient.newHttpClient().send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void simularConParametrosPersistidosYPublicarOpenApi() throws Exception {
        long cantidadInicial = repositorio.count();
        var respuesta = solicitar("/api/cliente/simulaciones",
                "{\"montoSolicitado\":10000.00,\"plazoMeses\":12,\"sistemaAmortizacion\":\"FRANCES\"}");
        assertThat(respuesta.statusCode()).isEqualTo(200);
        var contenido = json.readTree(respuesta.body());
        assertThat(contenido.get("teaReferencial").decimalValue()).isEqualByComparingTo("0.1550");
        assertThat(contenido.get("tablaAmortizacion").get("cuotas").size()).isEqualTo(12);
        assertThat(repositorio.count()).isEqualTo(cantidadInicial);
        var documentacion = solicitar("/v3/api-docs", null);
        assertThat(documentacion.statusCode()).isEqualTo(200);
        assertThat(json.readTree(documentacion.body()).get("paths").has("/api/cliente/simulaciones")).isTrue();
        var swagger = solicitar("/swagger-ui/index.html", null);
        assertThat(swagger.statusCode()).isEqualTo(200);
        assertThat(swagger.body()).contains("Swagger UI");
    }

    @Test
    void rechazarPlazoFraccionarioYCamposControladosPorServidor() throws Exception {
        var fraccion = solicitar("/api/cliente/simulaciones",
                "{\"montoSolicitado\":10000,\"plazoMeses\":12.5,\"sistemaAmortizacion\":\"FRANCES\"}");
        assertThat(fraccion.statusCode()).isEqualTo(400);
        var tasa = solicitar("/api/cliente/simulaciones",
                "{\"montoSolicitado\":10000,\"plazoMeses\":12,\"sistemaAmortizacion\":\"FRANCES\",\"teaReferencial\":0.01}");
        assertThat(tasa.statusCode()).isEqualTo(400);
    }

    @Test
    void excluirParametrosVencidosYFuturos() {
        transacciones.executeWithoutResult(estado -> {
            guardarParametro("PAR-99", "1", fecha.minusDays(10), fecha);
            guardarParametro("PAR-99", "2", fecha.plusDays(1), null);
        });
        assertThatThrownBy(() -> parametros.getValue("PAR-99", fecha)).isInstanceOf(ParameterNotFoundException.class);
        assertThat(parametros.getValue("PAR-99", fecha.minusDays(1))).isEqualByComparingTo("1");
        assertThat(parametros.getValue("PAR-99", fecha.plusDays(1))).isEqualByComparingTo("2");
    }

    @Test
    void respetarSeleccionDelServicioCompartidoDeMain() {
        transacciones.executeWithoutResult(estado -> {
            guardarParametro("PAR-99", "1", fecha.minusDays(10), null);
            guardarParametro("PAR-99", "2", fecha, null);
        });
        assertThat(parametros.getValue("PAR-99", fecha)).isEqualByComparingTo("2");
    }

    @Test
    void informarParametroAusenteSinExponerDetallesInternos() throws Exception {
        transacciones.executeWithoutResult(estado -> repositorio.deleteAll());
        var respuesta = solicitar("/api/cliente/simulaciones/parametros", null);
        assertThat(respuesta.statusCode()).isEqualTo(503);
        assertThat(respuesta.body()).doesNotContain("ParameterNotFoundException", "PAR-01");
    }
}
