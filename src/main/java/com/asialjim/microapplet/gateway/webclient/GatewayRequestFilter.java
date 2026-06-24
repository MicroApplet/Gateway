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

package com.asialjim.microapplet.gateway.webclient;

import com.asialjim.microapplet.web.client.MamsHttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class GatewayRequestFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        ClientRequest clientRequest = ClientRequest.from(request)
                .headers(headers -> {
                    headers.set(MamsHttpHeaders.HTTP_CLIENT_TYPE, MamsHttpHeaders.LOAD_BALANCE_CLIENT);

                    String traceId = MDC.get(MamsHttpHeaders.TRACE_ID);
                    if (StringUtils.isNotBlank(traceId))
                        headers.set(MamsHttpHeaders.TRACE_ID, traceId);

                    String sessionId = MDC.get(MamsHttpHeaders.SESSION_ID);
                    if (StringUtils.isNotBlank(sessionId))
                        headers.set(MamsHttpHeaders.SESSION_ID, sessionId);

                    String token = MDC.get(MamsHttpHeaders.USER_TOKEN_KEY);
                    if (StringUtils.isNotBlank(token))
                        headers.set(MamsHttpHeaders.USER_TOKEN_KEY, token);
                }).build();

        return next.exchange(clientRequest);
    }
}
