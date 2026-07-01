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

import com.asialjim.microapplet.session.SessionCtx;
import com.asialjim.microapplet.session.SessionRepository;
import com.asialjim.microapplet.gateway.context.AuthenticateResCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * 网关用户身份认证组件
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/27, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Component
public class AuthFilter implements GatewayFilter {
    public static final String NAME = "AuthFilter";

    public static class Config {
    }

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SESSION_ATTR = "web:attribute:user:session";

    private static final String[] TOKEN = {"authorization", MamsHttpHeaders.Authorization, MamsHttpHeaders.USER_TOKEN_KEY};

    private final SessionCtx authenticator;
    private final SessionRepository sessionRepository;

    public AuthFilter(SessionCtx authenticator) {
        this.authenticator = authenticator;
        this.sessionRepository = authenticator.sessionRepository();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        return Mono.deferContextual(ctxView -> {
            String trace = exchange.getAttribute(MamsHttpHeaders.TRACE_ID);

            return Mono.just(request)
                    .map(this::authorization)
                    .flatMap(this.authenticator::authMono)
                    .timeout(Duration.ofSeconds(5))
                    .switchIfEmpty(Mono.error(AuthenticateResCode.Failure::ex))
                    .flatMap(session -> {
                                try {

                                    if (StringUtils.isNotBlank(trace))
                                        MDC.put(MamsHttpHeaders.TRACE_ID, trace);

                                    log.info("\r\n用户认证成功：\r\n\t{}\r\nHeaders:\r\n\t{}", session, request.getHeaders());

                                    // 没有会话编号，认证失败
                                    String sessionId = session.getId();
                                    if (StringUtils.isBlank(sessionId))
                                        return Mono.error(AuthenticateResCode.Failure.ex(Collections.singletonList("会话编号[sessionId]获取失败")));

                                    // 没有会话信息，认证失败
                                    //noinspection ConstantValue
                                    if (Objects.isNull(session))
                                        return Mono.error(AuthenticateResCode.Failure.ex(Collections.singletonList("用户会话[session]获取失败")));

                                    // 会话信息中没有取到用户编号，认证失败
                                    String openid = session.getOpenid();
                                    if (StringUtils.isBlank(openid))
                                        return Mono.error(AuthenticateResCode.Failure.ex(Collections.singletonList("用户编号[openid]获取失败")));
                                    String token = session.getToken();
                                    String traceId = ctxView.get(MamsHttpHeaders.TRACE_ID);
                                    session.setTrace(traceId);
                                    ServerHttpRequest targetReq = exchange.getRequest()
                                            .mutate()
                                            .header(MamsHttpHeaders.SESSION_ID, sessionId)
                                            .header(MamsHttpHeaders.USER_TOKEN_KEY, session.getToken())
                                            .header(MamsHttpHeaders.Authorization, session.getToken())
                                            .header(MamsHttpHeaders.OPEN_ID, openid)
                                            .build();

                                    ServerWebExchange change = exchange.mutate().request(targetReq).response(response).build();
                                    change.getAttributes().put(SESSION_ATTR, session);
                                    change.getAttributes().put(MamsHttpHeaders.SESSION_ID, sessionId);
                                    return sessionRepository.saveMono(session)
                                            .then(Mono.defer(() -> chain.filter(change)
                                                    .contextWrite(ctx -> ctx.put(MamsHttpHeaders.USER_TOKEN_KEY, token))
                                                    .contextWrite(ctx -> ctx.put(MamsHttpHeaders.SESSION_ID, sessionId))));
                                } finally {
                                    MDC.clear();
                                }
                            }
                    );
        });

    }


    private Set<String> authorization(ServerHttpRequest request) {
        Set<String> tokens = new HashSet<>();
        Optional.ofNullable(request)
                .map(HttpMessage::getHeaders)
                .ifPresent(headers -> authorizationToken(headers, tokens));

        Optional.ofNullable(request)
                .map(ServerHttpRequest::getCookies)
                .map(Map::values)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .filter(cookie -> MamsHttpHeaders.USER_TOKEN_KEY.equalsIgnoreCase(cookie.getName()))
                .map(HttpCookie::getValue)
                .filter(StringUtils::isNotBlank)
                .forEach(tokens::add);
        if (CollectionUtils.isEmpty(tokens))
            AuthenticateResCode.TokenMiss.thr(List.of("检查Authorization 或 Cookie:" + MamsHttpHeaders.USER_TOKEN_KEY));

        return tokens;
    }

    private void authorizationToken(HttpHeaders headers, Set<String> tokens) {
        for (String s : TOKEN) {
            String value = headers.getFirst(s);
            if (StringUtils.isNotBlank(value))
                tokens.add(value.replaceFirst(BEARER_PREFIX, StringUtils.EMPTY));
        }
    }
}