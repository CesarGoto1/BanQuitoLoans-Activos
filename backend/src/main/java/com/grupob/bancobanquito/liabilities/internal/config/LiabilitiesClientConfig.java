package com.grupob.bancobanquito.liabilities.internal.config;
import com.grupob.bancobanquito.liabilities.internal.services.*;
import com.grupob.bancobanquito.liabilities.internal.exceptions.*;
import com.grupob.bancobanquito.liabilities.internal.config.*;
import com.grupob.bancobanquito.liabilities.internal.dto.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class LiabilitiesClientConfig {

    @Bean
    RestClient liabilitiesRestClient(@Value("${liabilities.url}") String url,
                                 @Value("${liabilities.timeout-seconds}") long segundos) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(segundos));
        fabrica.setReadTimeout(Duration.ofSeconds(segundos));
        return RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }
}

















