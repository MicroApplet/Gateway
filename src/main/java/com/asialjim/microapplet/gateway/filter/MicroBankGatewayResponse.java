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

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

public class MicroBankGatewayResponse extends ServerHttpResponseDecorator {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String HEADER_REQ_TIME = "X-Gateway-Req-Time";
    private static final String HEADER_RES_TIME = "X-Gateway-Res-Time";
    private static final String HEADER_COST_TIME = "X-Cost-Time";

    private final ServerWebExchange exchange;
    private final LocalDateTime reqTime;
    private final boolean addHeader;

    public MicroBankGatewayResponse(ServerWebExchange exchange) {
        this(true, exchange);
    }

    public MicroBankGatewayResponse(boolean addHeader, ServerWebExchange exchange) {
        super(exchange.getResponse());
        this.exchange = exchange;
        this.reqTime = LocalDateTime.now();
        this.addHeader = addHeader;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        if (addHeader) addHeader();
        return super.writeWith(body);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        if (addHeader) addHeader();
        return super.writeAndFlushWith(body);
    }

    private void addHeader() {
        String traceId = exchange.getAttribute(MamsHttpHeaders.TRACE_ID);
        if (StringUtils.isBlank(traceId))
            traceId = "Empty Trace Id";
        String sessionId = exchange.getAttribute(MamsHttpHeaders.SESSION_ID);
        if (StringUtils.isBlank(sessionId))
            sessionId = "Empty Session Id";

        LocalDateTime resTime = LocalDateTime.now();
        HttpHeaders headers = this.getHeaders();

        long millis = java.time.Duration.between(reqTime, resTime).toMillis();
        headers.set(HEADER_REQ_TIME, reqTime.format(DTF));
        headers.set(HEADER_RES_TIME, resTime.format(DTF));
        headers.set(HEADER_COST_TIME, millis + " ms");
        headers.set(MamsHttpHeaders.TRACE_ID, traceId);
        headers.set(MamsHttpHeaders.SESSION_ID, sessionId);
    }

    public void resHeader(StringJoiner sj) {
        HttpStatusCode statusCode = getStatusCode();
        HttpHeaders headers = getHeaders();
        sj.add("<<< " + statusCode);
        headers.forEach((k, strings) -> sj.add("\t" + k + "=" + String.join(";", strings)));
    }
}
