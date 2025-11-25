/*
 *    Copyright 2014-2025 <a href="mailto:asialjim@qq.com">Asial Jim</a>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.asialjim.microapplet.gateway.filter;

import com.asialjim.microapplet.common.cons.Headers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 全局链路追踪组件
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/5/6, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
public class GlobalTraceFilter implements GatewayFilter, Ordered {
    private static final String REQUEST_TIME = "_request_time_";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = generateTraceId();
        Map<String, Object> attributes = exchange.getAttributes();

        // 使用更轻量的时间记录方式
        long startTime = System.nanoTime();
        attributes.put("_request_start_nano_", startTime);
        attributes.put(REQUEST_TIME, LocalDateTime.now().format(FORMATTER));
        attributes.put("_trace_id_", traceId); // 存储traceId供后续使用

        // 添加Trace ID到请求头（传递给下游服务）
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(Headers.TraceId, traceId)
                .header(Headers.TRACE_ID, traceId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // 记录请求日志
        if (log.isDebugEnabled())
            log.debug("Request started - TraceID: {}, Method: {}, Path: {}", traceId, mutatedRequest.getMethod(), mutatedRequest.getPath());

        return chain.filter(mutatedExchange)
                .doOnSuccess(unused -> logRequestCompletion(exchange, false, null))
                .doOnError(error -> logRequestCompletion(exchange, true, error))
                .then(Mono.fromRunnable(() -> {
                    // 在响应提交前添加响应头（如果启用）
                    if (!exchange.getResponse().isCommitted()) {
                        exchange.getResponse().getHeaders().set(Headers.TraceId, traceId);
                        String requestTime = exchange.getAttribute(REQUEST_TIME);
                        if (StringUtils.isNotBlank(requestTime))
                            //noinspection UastIncorrectHttpHeaderInspection
                            exchange.getResponse().getHeaders().set("X-Request-Time", requestTime);
                        Long startTime1 = exchange.getAttribute("_request_start_nano_");
                        if (Objects.nonNull(startTime1)) {
                            long duration = (System.nanoTime() - startTime1) / 1_000_000; // 转换为毫秒
                            exchange.getResponse().getHeaders().set("X-Response-Time", duration + "ms");
                        }
                    } else {
                        if (log.isDebugEnabled())
                            log.debug("Response had Commited");
                    }
                }));
    }

    private void logRequestCompletion(ServerWebExchange exchange, boolean isError, Throwable error) {
        Long startTime = exchange.getAttribute("_request_start_nano_");
        String traceId = exchange.getAttribute("_trace_id_");

        if (startTime != null && traceId != null) {
            long duration = (System.nanoTime() - startTime) / 1_000_000; // 转换为毫秒

            if (isError) {
                log.error("Request failed - TraceID: {}, Duration: {}ms, Error: {}",
                        traceId, duration, Optional.ofNullable(error).map(Throwable::getMessage).orElse("Unknown error"));
            } else {
                if (log.isDebugEnabled())
                    log.info("Request completed - TraceID: {}, Duration: {}ms, Status: {}",
                            traceId, duration, exchange.getResponse().getStatusCode());
            }
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}