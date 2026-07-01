/*
 * Copyright (c) TFB 2019 - 2026.  All rights reserved.
 */

package com.asialjim.microapplet.gateway.config;

import com.asialjim.microapplet.web.client.adapter.WebClientHttpExchangeAdapterFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;

@Slf4j
@Configuration
public class WebFluxHttpExchangeAdapterFactory
        extends WebClientHttpExchangeAdapterFactory {

    @Resource
    @Qualifier("loadBalancedRestClientBuilder")
    private WebClient.Builder loadBalancedWebClientBuilder;

    @Override
    public HttpExchangeAdapter build(String serviceName) {
        //noinspection HttpUrlsUsage
        WebClient.Builder build = loadBalancedWebClientBuilder.baseUrl("http://" + serviceName);

        if (log.isDebugEnabled()) {
            build = build.filters(functions -> {
                for (ExchangeFilterFunction function : functions) {
                    log.debug("WebClient过滤器 {}", function);
                }
            });
        }

        WebClient client = build.build();
        return WebClientAdapter.create(client);
    }
}