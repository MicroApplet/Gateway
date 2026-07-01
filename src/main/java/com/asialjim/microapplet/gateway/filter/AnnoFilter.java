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

import com.asialjim.microapplet.session.Session;
import com.asialjim.microapplet.session.SessionCtx;
import com.asialjim.microapplet.session.SessionRepository;
import com.asialjim.microapplet.gateway.context.AuthenticateResCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;

/**
 * 开放路由访问会话组件
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/27, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Component
public class AnnoFilter implements GatewayFilter {
    public static final String NAME = "AnnoFilter";

    public static class Config {
    }

    private final SessionRepository sessionRepository;

    public AnnoFilter(SessionCtx sessionCtx) {
        this.sessionRepository = sessionCtx.sessionRepository();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();

        return Mono.deferContextual(ctx ->
                        Mono.just(ctx.get(MamsHttpHeaders.TRACE_ID))
                                .cast(String.class)
                                .map(_ -> Session.tourist())
                )
                .flatMap(session -> {
                    try {
                        String trace = exchange.getAttribute(MamsHttpHeaders.TRACE_ID);

                        if (StringUtils.isNotBlank(trace))
                            MDC.put(MamsHttpHeaders.TRACE_ID, trace);

                        log.info("开放路由访问：{}", session.getId());
                        // 认证失败
                        String sessionId = session.getId();
                        if (StringUtils.isBlank(sessionId))
                            return Mono.error(AuthenticateResCode.Failure.ex(Collections.singletonList("会话编号[sessionId]获取失败")));

                        //noinspection ConstantValue
                        if (Objects.isNull(session))
                            return Mono.error(AuthenticateResCode.Failure.ex(Collections.singletonList("用户会话[session]获取失败")));
                        String openid = session.getOpenid();
                        if (StringUtils.isBlank(openid))
                            return Mono.error(AuthenticateResCode.Failure.ex(Collections.singletonList("用户编号[openid]获取失败")));

                        // 认证成功
                        ServerHttpRequest targetReq = exchange.getRequest()
                                .mutate()
                                .header(MamsHttpHeaders.SESSION_ID, sessionId)
                                .header(MamsHttpHeaders.USER_TOKEN_KEY, session.getToken())
                                .header(MamsHttpHeaders.Authorization, session.getToken())
                                .header(MamsHttpHeaders.OPEN_ID, openid)
                                .build();

                        ServerWebExchange change = exchange.mutate().request(targetReq).response(response).build();
                        change.getAttributes().put(SessionCtx.userSessionAttribute, session);
                        change.getAttributes().put(MamsHttpHeaders.SESSION_ID, sessionId);

                        return sessionRepository.saveMono(session)
                                .then(Mono.defer(() -> chain.filter(change)
                                        .contextWrite(ctx -> {
                                            if (StringUtils.isNotBlank(session.getToken()))
                                                return ctx.put(MamsHttpHeaders.USER_TOKEN_KEY, session.getToken());
                                            return ctx;
                                        })
                                        //.contextWrite(ctx -> ctx.put(MamsHttpHeaders.USER_TOKEN_KEY, session.getToken()))
                                        .contextWrite(ctx -> ctx.put(MamsHttpHeaders.SESSION_ID, sessionId))));
                    } finally {
                        MDC.clear();
                    }
                });
    }
}