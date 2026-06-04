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

package com.asialjim.microapplet.gateway.filter.response;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class ResponseAdaptor implements ClientHttpResponse {
    private final Flux<? extends DataBuffer> flux;
    private final HttpHeaders headers;
    private final HttpStatusCode httpStatusCode;
    private final MultiValueMap<String, ResponseCookie> cookieMultiValueMap;

    public ResponseAdaptor(Publisher<? extends DataBuffer> body,
                           HttpHeaders headers, HttpStatusCode httpStatusCode,
                           MultiValueMap<String, ResponseCookie> cookieMultiValueMap) {
        this.headers = headers;
        switch (body) {
            case Flux<? extends DataBuffer> bufferFlux -> this.flux = bufferFlux;
            case Mono<? extends DataBuffer> bufferMono -> this.flux = bufferMono.flux();
            default -> this.flux = Flux.fromArray(new DataBuffer[0]);
        }
        this.cookieMultiValueMap = cookieMultiValueMap;
        this.httpStatusCode = httpStatusCode;
    }

    @Override
    public HttpStatusCode getStatusCode() {
        return this.httpStatusCode;
    }

    @Override
    public MultiValueMap<String, ResponseCookie> getCookies() {
        return this.cookieMultiValueMap;
    }

    @Override
    public Flux<DataBuffer> getBody() {
        //noinspection unchecked
        return (Flux<DataBuffer>) this.flux;
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }
}