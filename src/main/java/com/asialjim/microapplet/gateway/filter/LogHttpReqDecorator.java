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

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

public class LogHttpReqDecorator extends ServerHttpRequestDecorator {
    private final ServerHttpRequest request;
    private final AtomicReference<StringBuffer> reference;

    public LogHttpReqDecorator(ServerWebExchange exchange) {
        super(exchange.getRequest());
        this.request = getDelegate();
        this.reference = new AtomicReference<>();
        reference.set(new StringBuffer());
    }

    public Flux<String> requestLogFlux() {
        return Flux.concat(Mono.just(requestLine()), Mono.just(requestHeader()), requestBody());
    }

    private Mono<String> requestBody() {
        StringBuffer stringBuffer = this.reference.get();
        String string = stringBuffer.toString();
        if (StringUtils.isBlank(string))
            return Mono.empty();

        String body = ">>> 请求体: \r\n\t" + string;
        return Mono.just(body);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Flux<DataBuffer> getBody() {
        Flux<DataBuffer> body = super.getBody();
        return body.doOnNext(buffer -> {
            // 保留DataBuffer用于日志记录
            DataBuffer retain = DataBufferUtils.retain(buffer);
            try {
                String string = retain.toString(StandardCharsets.UTF_8);
                StringBuffer stringBuffer = reference.get();
                stringBuffer.append(string);
            } finally {
                // 确保在日志记录后释放保留的DataBuffer
                DataBufferUtils.release(retain);
            }
        });
    }

    private String requestHeader() {
        HttpHeaders headers = request.getHeaders();
        StringJoiner headJoiner = new StringJoiner("\r\n\t\t");
        headJoiner.add(StringUtils.EMPTY);
        headers.forEach((k, v) -> headJoiner.add(k + " -> " + String.join(",", v)));

        return ">>> 请求头: " + headJoiner;
    }

    private String requestLine() {
        String requestLine = request.getMethod() + " " + request.getPath() + " HTTP/1.1";
        return ">>> 请求行: " + requestLine;
    }
}