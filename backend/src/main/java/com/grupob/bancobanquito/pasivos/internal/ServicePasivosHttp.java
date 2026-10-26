package com.grupob.bancobanquito.pasivos.internal;

import com.grupob.bancobanquito.pasivos.Cuenta;
import com.grupob.bancobanquito.pasivos.Cliente;
import com.grupob.bancobanquito.pasivos.InstruccionDesembolso;
import com.grupob.bancobanquito.pasivos.ServicePasivos;
import com.grupob.bancobanquito.pasivos.PasivosNoDisponibleException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Profile("!simulated")
class ServicePasivosHttp implements ServicePasivos {

    private static final String INSTRUCTION_VERSION = "1.0";
    private static final String ACTIVE_ACCOUNT = "ACTIVA";

    private final RestClient restClient;

    ServicePasivosHttp(@Value("${liabilities.url}") String liabilitiesUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(liabilitiesUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<Cliente> findCustomer(Integer idCliente) {
        try {
            ClientePasivos customer = restClient.get()
                    .uri("/api/clientes?idCliente={idCliente}", idCliente)
                    .retrieve()
                    .body(ClientePasivos.class);
            return Optional.ofNullable(customer)
                    .map(found -> new Cliente(found.idCliente(), found.cedulaCliente(),
                            found.nombresCliente(), found.apellidosCliente()));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new PasivosNoDisponibleException(e);
        } catch (RestClientException e) {
            throw new PasivosNoDisponibleException(e);
        }
    }

    @Override
    public Optional<Cliente> findCustomerByNationalId(String cedula) {
        try {
            ClientePasivos customer = restClient.get()
                    .uri("/api/clientes/{cedula}", cedula)
                    .retrieve()
                    .body(ClientePasivos.class);
            return Optional.ofNullable(customer)
                    .map(found -> new Cliente(found.idCliente(), found.cedulaCliente(),
                            found.nombresCliente(), found.apellidosCliente()));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new PasivosNoDisponibleException(e);
        } catch (RestClientException e) {
            throw new PasivosNoDisponibleException(e);
        }
    }

    @Override
    public List<Cuenta> findActiveAccounts(Integer idCliente) {
        Optional<Cliente> customer = findCustomer(idCliente);
        if (customer.isEmpty()) {
            return List.of();
        }
        List<CuentaPasivos> cuentas;
        try {
            cuentas = restClient.get()
                    .uri("/api/clientes/{cedula}/cuentas", customer.get().cedula())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CuentaPasivos>>() {
                    });
        } catch (RestClientException e) {
            throw new PasivosNoDisponibleException(e);
        }
        if (cuentas == null) {
            return List.of();
        }
        return cuentas.stream()
                .filter(account -> ACTIVE_ACCOUNT.equals(account.estado()))
                .map(account -> new Cuenta(account.numeroCuenta(), account.tipo()))
                .toList();
    }

    @Override
    public void creditDisbursement(InstruccionDesembolso instruction) {
        try {
            restClient.post()
                    .uri("/api/desembolsos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DesembolsoPasivos(INSTRUCTION_VERSION, instruction.numeroOperacion(),
                            instruction.cedulaCliente(), instruction.cuentaDestino(),
                            instruction.montoAprobado(), instruction.contribucionSolca(), instruction.montoNeto(),
                            instruction.fechaDesembolso(), "Desembolso de la operación " + instruction.numeroOperacion()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new PasivosNoDisponibleException(e);
        }
    }
}
