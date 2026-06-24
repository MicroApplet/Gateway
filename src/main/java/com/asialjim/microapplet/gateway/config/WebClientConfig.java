/*
 * Copyright 2014-2025 <a href="mailto:asialjim@qq.com">Asial Jim</a>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.asialjim.microapplet.gateway.config;

import com.asialjim.microapplet.commons.standard.utils.JsonUtil;
import com.asialjim.microapplet.gateway.webclient.GatewayRequestFilter;
import com.asialjim.microapplet.gateway.webclient.GatewayResponseFilter;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 配置，参考 demo webflux 项目的 MicroBankWebClientConfig。
 */
@Configuration
public class WebClientConfig {

    @Resource
    private GatewayRequestFilter gatewayRequestFilter;
    @Resource
    private GatewayResponseFilter gatewayResponseFilter;

    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean(name = "loadBalancedWebClientBuilder")
    public WebClient.Builder loadBalancedWebClientBuilder() {
        ExchangeStrategies jacksonStrategy = ExchangeStrategies.builder()
                .codecs(conf -> {
                    conf.defaultCodecs().jacksonJsonDecoder(
                            new JacksonJsonDecoder(JsonUtil.instance.objectMapper()));
                    conf.defaultCodecs().jacksonJsonEncoder(
                            new JacksonJsonEncoder(JsonUtil.instance.objectMapper()));
                }).build();

        return WebClient.builder()
                .defaultHeaders(headers -> {
                    headers.remove(HttpHeaders.ACCEPT);
                    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                })
                .exchangeStrategies(jacksonStrategy)
                .filter(gatewayRequestFilter)
                .filter(gatewayResponseFilter);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "webClientBuilder")
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
