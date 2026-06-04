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

import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Setter
public class DocumentFilter implements GatewayFilter, Ordered {
    public static final String DOCUMENT_REQUEST_URL_ATTR = "_is_document_req_attr_";

    private String context;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerWebExchangeUtils.addOriginalRequestUrl(exchange, request.getURI());
        String path = request.getURI().getRawPath();
        boolean tag = StringUtils.startsWith(path,context) && StringUtils.endsWithAny(path,".html",".xhtml",".css",".js");
        exchange.getAttributes().put(DOCUMENT_REQUEST_URL_ATTR, tag);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -101; // 比 auth 高一个优先级
    }


    @Data
    @Accessors(chain = true)
    public static class Config {
        private String context;
    }
}