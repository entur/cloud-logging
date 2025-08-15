package org.entur.example.web.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.httpclient5.LogbookHttpRequestInterceptor;
import org.zalando.logbook.httpclient5.LogbookHttpResponseInterceptor;

@Configuration
public class ClientConfig {

    @Bean
    public CloseableHttpClient httpClient(Logbook logbook)  {
        https://github.com/zalando/logbook/tree/main?tab=readme-ov-file#http-client-5

        return HttpClientBuilder.create()
                .addRequestInterceptorFirst(new LogbookHttpRequestInterceptor(logbook))
                .addResponseInterceptorFirst(new LogbookHttpResponseInterceptor())
                .build();
    }
}
