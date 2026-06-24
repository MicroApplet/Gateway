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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * 网关链路追踪日志记录组件
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/27, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Component
public class TraceFilter implements GatewayFilter {
    public static final String name = "TraceFilter";


    public static class Config { }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return Mono.just(exchange)
                .map(ServerWebExchange::getRequest)
                .flatMap(request -> {
                    String traceId = request.getHeaders().getFirst(MamsHttpHeaders.TRACE_ID);
                    if (StringUtils.isBlank(traceId))
                        traceId = request.getHeaders().getFirst("traceid");
                    if (StringUtils.isBlank(traceId))
                        traceId = UUID.randomUUID().toString().replace("-", StringUtils.EMPTY);

                    String finalTraceId = traceId;
                    ServerHttpRequest targetReq = request.mutate().header(MamsHttpHeaders.TRACE_ID, traceId).build();

                    ServerWebExchange targetEx = exchange.mutate().request(targetReq).build();
                    Map<String, Object> attributes = targetEx.getAttributes();
                    attributes.put(MamsHttpHeaders.TRACE_ID, traceId);
                    try {
                        HttpMethod method = request.getMethod();
                        URI uri = request.getURI();
                        MDC.put(MamsHttpHeaders.TRACE_ID, traceId);
                        if (log.isDebugEnabled()) {
                            log.info("=== [{}]: {} ===", method, uri);
                            HttpHeaders headers = request.getHeaders();
                            headers.forEach((k, strings) -> {
                                if (CollectionUtils.isNotEmpty(strings)) {
                                    log.info("\tHeader: {}={}", k, String.join(";", strings));
                                }
                            });
                        }
                    } finally {
                        MDC.clear();
                    }

                    return chain.filter(targetEx).contextWrite(ctx -> ctx.put(MamsHttpHeaders.TRACE_ID, finalTraceId));
                });
    }
}