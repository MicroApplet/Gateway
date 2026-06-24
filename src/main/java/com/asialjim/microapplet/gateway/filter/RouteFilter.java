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

package com.asialjim.microapplet.gateway.filter;
import com.asialjim.microapplet.web.client.MamsHttpHeaders;

import com.asialjim.microapplet.commons.standard.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 网关路由选择日志记录组件
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/27, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Component
public class RouteFilter implements GatewayFilter {
    public static final String name = "RouteFilter";

    public static class Config {
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String trace = exchange.getAttribute(MamsHttpHeaders.TRACE_ID);
        return Mono.defer(() -> {
            StringJoiner sj = new StringJoiner("\r\n");
            sj.add(StringUtils.EMPTY);

            ServerHttpRequest request = exchange.getRequest();
            HttpMethod method = request.getMethod();
            URI uri = request.getURI();
            sj.add(">>> [" + method + "] " + uri);
            HttpHeaders headers = request.getHeaders();
            headers.forEach((k, strings) -> {
                if (CollectionUtils.isNotEmpty(strings)) {
                    sj.add("\t" + k + "=" + String.join(";", strings));
//
//                    if (log.isDebugEnabled())
//                        sj.add("\t" + k + "=" + String.join(";", strings));
//                    else
//                        //noinspection StatementWithEmptyBody
//                        if (contains(k))
//                            sj.add("\t" + k + "=" + String.join(";", strings));
//                        else {
//                            // do nothing here
//                        }
//
                }
            });

            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeInfo = Objects.nonNull(route) ? route.getUri().toString() : "No route match";
            sj.add("=== Route to " + routeInfo);


            MicroBankGatewayResponse response = new MicroBankGatewayResponse(false, exchange);
            ServerWebExchange webExchange = exchange.mutate().response(response).build();

            return chain.filter(webExchange)
                    .doOnSuccess(unused -> response.resHeader(sj))
                    .doOnCancel(() -> sj.add("XXX Request Canceled"))
                    .doOnError(t -> {
                        if (t instanceof BusinessException apiException)
                            sj.add("XXX Request Error: " + apiException.detailMessage());
                        else
                            sj.add("XXX Request Error: " + t.getMessage());
                    })
                    .doFinally(signalType -> {

                        try {
                            if (StringUtils.isNotBlank(trace))
                                MDC.put(MamsHttpHeaders.TRACE_ID, trace);

                            log.info(sj.toString());
                        } finally {
                            MDC.clear();
                        }
                    });
        });
    }

    private static boolean contains(String name) {
        return names.stream().anyMatch(item -> StringUtils.equalsIgnoreCase(name, item));
    }

    private static final List<String> names =
            List.of(
                    HttpHeaders.USER_AGENT,
                    HttpHeaders.ACCEPT,
                    HttpHeaders.CONTENT_LANGUAGE,
                    HttpHeaders.CONTENT_LENGTH,
                    HttpHeaders.CONTENT_TYPE,
                    "x-remote-ip",
                    "x-forwarded-for"
            );
}