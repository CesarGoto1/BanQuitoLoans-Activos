package com.grupob.bancobanquito.liabilities.internal;

import com.grupob.bancobanquito.liabilities.Account;
import com.grupob.bancobanquito.liabilities.Customer;
import com.grupob.bancobanquito.liabilities.DisbursementInstruction;
import com.grupob.bancobanquito.liabilities.LiabilitiesService;
import com.grupob.bancobanquito.liabilities.LiabilitiesUnavailableException;
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
class HttpLiabilitiesService implements LiabilitiesService {

    private static final String INSTRUCTION_VERSION = "1.0";
    private static final String ACTIVE_ACCOUNT = "ACTIVA";

    private final RestClient restClient;

    HttpLiabilitiesService(@Value("${liabilities.url}") String liabilitiesUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(liabilitiesUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<Customer> findCustomer(Integer customerId) {
        try {
            LiabilitiesCustomer customer = restClient.get()
                    .uri("/api/customers?customerId={customerId}", customerId)
                    .retrieve()
                    .body(LiabilitiesCustomer.class);
            return Optional.ofNullable(customer)
                    .map(found -> new Customer(found.customerId(), found.identificationNumber(),
                            found.firstNames(), found.lastNames()));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new LiabilitiesUnavailableException(e);
        } catch (RestClientException e) {
            throw new LiabilitiesUnavailableException(e);
        }
    }

    @Override
    public Optional<Customer> findCustomerByNationalId(String nationalId) {
        try {
            LiabilitiesCustomer customer = restClient.get()
                    .uri("/api/customers/{nationalId}", nationalId)
                    .retrieve()
                    .body(LiabilitiesCustomer.class);
            return Optional.ofNullable(customer)
                    .map(found -> new Customer(found.customerId(), found.identificationNumber(),
                            found.firstNames(), found.lastNames()));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new LiabilitiesUnavailableException(e);
        } catch (RestClientException e) {
            throw new LiabilitiesUnavailableException(e);
        }
    }

    @Override
    public List<Account> findActiveAccounts(Integer customerId) {
        Optional<Customer> customer = findCustomer(customerId);
        if (customer.isEmpty()) {
            return List.of();
        }
        List<LiabilitiesAccount> accounts;
        try {
            accounts = restClient.get()
                    .uri("/api/customers/{nationalId}/accounts", customer.get().nationalId())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<LiabilitiesAccount>>() {
                    });
        } catch (RestClientException e) {
            throw new LiabilitiesUnavailableException(e);
        }
        if (accounts == null) {
            return List.of();
        }
        return accounts.stream()
                .filter(account -> ACTIVE_ACCOUNT.equals(account.status()))
                .map(account -> new Account(account.accountNumber(), account.type()))
                .toList();
    }

    @Override
    public void creditDisbursement(DisbursementInstruction instruction) {
        try {
            restClient.post()
                    .uri("/api/disbursements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LiabilitiesDisbursement(INSTRUCTION_VERSION, instruction.loanNumber(),
                            instruction.customerNationalId(), instruction.destinationAccount(),
                            instruction.approvedAmount(), instruction.solcaContribution(), instruction.netAmount(),
                            instruction.disbursementDate(), "Desembolso de la operación " + instruction.loanNumber()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new LiabilitiesUnavailableException(e);
        }
    }
}
