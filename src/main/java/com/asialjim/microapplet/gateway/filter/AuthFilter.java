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

import com.asialjim.microapplet.gateway.cloud.AuthServiceLoadBalancerConfig;
import com.asialjim.microapplet.common.cons.Headers;
import com.asialjim.microapplet.common.cons.WebCons;
import com.asialjim.microapplet.common.context.Res;
import com.asialjim.microapplet.common.context.ResCode;
import com.asialjim.microapplet.common.context.Result;
import com.asialjim.microapplet.common.exception.RsEx;
import com.asialjim.microapplet.common.security.MamsSession;
import com.asialjim.microapplet.common.utils.JsonUtil;
import com.asialjim.microapplet.gateway.config.AuthServerProperty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 用户认证过滤器
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/9/24, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@AllArgsConstructor
public class AuthFilter implements GatewayFilter, Ordered {
    private AuthServerProperty authServerProperty;
    private WebClient.Builder webClientBuilder;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Boolean isDocumentReq = exchange.getAttribute(DocumentFilter.DOCUMENT_REQUEST_URL_ATTR);
        if (Boolean.TRUE.equals(isDocumentReq))
            return chain.filter(exchange);

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 提取令牌
        String token = extractToken(request);

        if (StringUtils.isBlank(token))
            return unauthorizedResponse(exchange, Res.UserAuthTokenMissing, "缺少认证令牌");

        // 调用认证服务验证令牌
        return session(token)
                .timeout(Duration.ofSeconds(5))
                .flatMap(session -> {
                    // 认证失败
                    if (StringUtils.isBlank(session.getUserid()))
                        return unauthorizedResponse(exchange, Res.UserAuthFailure401Thr, "认证失败");

                    // 认证成功
                    ServerHttpRequest targetRequest = exchange.getRequest()
                            .mutate()
                            .header(Headers.CURRENT_SESSION, JsonUtil.instance.toStr(session))
                            .build();
                    response.getHeaders().set(Headers.SessionId, session.getId());
                    return chain.filter(exchange.mutate().request(targetRequest).response(response).build());
                })
                .onErrorResume(e -> {
                    log.warn("认证服务不可用：{}",e.getMessage(),e);
                    if (e instanceof RsEx rsEx)
                        return unauthorizedResponse(exchange, rsEx);
                    return unauthorizedResponse(exchange, Res.UserAuthFailure401, "认证服务不可用");
                });
    }

    private String extractToken(ServerHttpRequest request) {
        // 从Authorization头提取
        String authHeader = request.getHeaders().getFirst(Headers.AUTHORIZATION);
        if (StringUtils.isNotBlank(authHeader))
            return authHeader.replaceFirst(WebCons.BEARER_PREFIX, StringUtils.EMPTY);

        authHeader = request.getHeaders().getFirst(Headers.USER_TOKEN);
        if (StringUtils.isNotBlank(authHeader))
            return authHeader.replaceFirst(WebCons.BEARER_PREFIX, StringUtils.EMPTY);

        // 从Cookie提取
        HttpCookie cookie = request.getCookies().getFirst(Headers.USER_TOKEN);

        if (Objects.nonNull(cookie))
            return cookie.getValue();


        return null;
    }

    private Mono<MamsSession> session(String token) {
        return webClientBuilder.build().get()
                .uri(this.authServerProperty.authUrl(token))
                .header(Headers.CLIENT_TYPE, Headers.CLOUD_CLIENT)
                .retrieve()
                .onStatus(HttpStatusCode::isError, AuthServiceLoadBalancerConfig.rsExFunction())
                .toEntity(String.class)
                .mapNotNull(HttpEntity::getBody)
                .mapNotNull(s -> JsonUtil.instance.toBean(s, MamsSession.class));
    }


    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, ResCode resCode, String... errs) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        Result<Object> result = resCode.result();
        if (ArrayUtils.isNotEmpty(errs)) {
            List<String> errs1 = result.getErrs();
            errs1.addAll(Arrays.asList(errs));
            result.setErrs(errs1);
        }

        String json = JsonUtil.instance.toStr(result);

        byte[] bits = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bits);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级
    }
}