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
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 日志处理器
 * <pre>
 *     提供统一的基于网关的日志记录以及链路追踪、请求时间、响应时间、请求耗时等信息
 * </pre>
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/12/2, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
public class LoggingFilter implements GatewayFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String traceId = generateTraceId(exchange);
        final StopWatch stopWatch = new StopWatch();
        final LogHttpReqDecorator decoratedRequest = new LogHttpReqDecorator(exchange);
        final LogHttpResDecorator decoratedResponse = new LogHttpResDecorator(traceId, stopWatch, exchange);

        final ServerWebExchange mutatedExchange = exchange.mutate()
                .request(decoratedRequest.mutate()
                        .header(Headers.TraceId, traceId)
                        .header(Headers.TRACE_ID, traceId)
                        .build()
                )
                .response(decoratedResponse)
                .build();

        final StringJoiner logJoiner = new StringJoiner("\r\n").add(StringUtils.EMPTY);
        return chain.filter(mutatedExchange)
                .then(Mono.defer(() -> decoratedRequest.requestLogFlux()
                        .filter(StringUtils::isNotBlank)
                        .doOnNext(logJoiner::add)
                        .then()
                ))
                .then(Mono.fromRunnable(() -> logJoiner.add("=== 请求中 ===")))
                .then(Mono.defer(() -> decoratedResponse.responseLogFlux()
                        .filter(StringUtils::isNotBlank)
                        .doOnNext(logJoiner::add)
                        .then()
                ))
                .then(Mono.fromRunnable(() -> logJoiner.add(">>> 共耗时 <<<\t" + stopWatch.getTime(TimeUnit.MILLISECONDS) + " Milli Seconds")))
                .then()
                .onErrorResume(e -> Mono.fromRunnable(() -> logJoiner.add("XXX 请求处理异常: " + e.getMessage())).then(Mono.error(e)))
                .doFinally(signalType -> {
                    try {
                        MDC.put(Headers.TRACE_ID, traceId);
                        log.info(logJoiner.add(StringUtils.EMPTY).toString());
                    } finally {
                        MDC.clear();
                    }
                });
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    private String generateTraceId(ServerWebExchange exchange) {
        String trace = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        exchange.getAttributes().put(Headers.TraceId,trace);
        return trace;
    }
}