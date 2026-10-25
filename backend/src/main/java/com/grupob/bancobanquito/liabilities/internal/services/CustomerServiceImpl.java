package com.grupob.bancobanquito.liabilities.internal.services;
import com.grupob.bancobanquito.liabilities.internal.services.*;
import com.grupob.bancobanquito.liabilities.internal.exceptions.*;
import com.grupob.bancobanquito.liabilities.internal.config.*;
import com.grupob.bancobanquito.liabilities.internal.dto.*;
import com.grupob.bancobanquito.liabilities.CustomerNotFoundException;
import com.grupob.bancobanquito.liabilities.CustomerSummary;
import com.grupob.bancobanquito.liabilities.CustomerService;
import com.grupob.bancobanquito.liabilities.LiabilitiesUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final RestClient restClient;

    public CustomerServiceImpl(@Qualifier("liabilitiesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CustomerSummary findByIdentification(String cedula) {
        try {
            RemoteCustomerResponse remoto = restClient.get()
                    .uri("/api/clientes/{cedula}", cedula)
                    .retrieve()
                    .body(RemoteCustomerResponse.class);
            if (remoto == null || remoto.idCliente() == null) {
                throw new CustomerNotFoundException("No se encontró el cliente.");
            }
            return new CustomerSummary(remoto.idCliente(), cedula);
        } catch (HttpClientErrorException.NotFound e) {
            throw new CustomerNotFoundException("No se encontró el cliente.");
        } catch (RestClientException e) {
            log.error("Core de Pasivos no disponible al fetch al cliente {}", maskIdentification(cedula), e);
            throw new LiabilitiesUnavailableException(
                    "El servicio de clientes no está disponible. Intente nuevamente en unos minutos.", e);
        }
    }

    private static String maskIdentification(String cedula) {
        return cedula != null && cedula.length() == 10
                ? cedula.substring(0, 2) + "******" + cedula.substring(8)
                : "**********";
    }
}

















