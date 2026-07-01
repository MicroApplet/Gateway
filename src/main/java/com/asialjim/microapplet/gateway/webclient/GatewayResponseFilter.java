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

import com.asialjim.microapplet.commons.standard.exception.BusinessException;
import com.asialjim.microapplet.web.client.MamsHttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class GatewayResponseFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {

        return next.exchange(request).flatMap(response -> {
            int status = response.statusCode().value();
            String success = response.headers().asHttpHeaders().getFirst(MamsHttpHeaders.RES_SUCCESS);
            if (StringUtils.isNotBlank(success) && Boolean.parseBoolean(success))
                return Mono.just(response);

            String code = response.headers().asHttpHeaders().getFirst(MamsHttpHeaders.RES_CODE);
            String msg = response.headers().asHttpHeaders().getFirst(MamsHttpHeaders.RES_MSG);
            String errsStr = response.headers().asHttpHeaders().getFirst(MamsHttpHeaders.RES_ERRS);
            List<String> errs =  new ArrayList<>();
            if (StringUtils.isNotBlank(errsStr)){
                errsStr = URLDecoder.decode(errsStr, StandardCharsets.UTF_8);
                errsStr = errsStr.replace("[",StringUtils.EMPTY).replace("]",StringUtils.EMPTY);
                String[] split = errsStr.split(",");
                for (String s : split) {
                    errs.add(s.replace("\"",StringUtils.EMPTY));
                }
            }

            log.warn("下游服务返回错误: status={}, code={}, msg={}", status, code, msg);

            return Mono.error(new BusinessException(status, code, msg, null, errs));
        });
    }
}
