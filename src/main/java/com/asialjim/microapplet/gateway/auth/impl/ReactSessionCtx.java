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

package com.asialjim.microapplet.gateway.auth.impl;

import com.asialjim.microapplet.commons.standard.utils.SessionTokenUtil;
import com.asialjim.microapplet.session.Session;
import com.asialjim.microapplet.session.SessionCtx;
import com.asialjim.microapplet.session.SessionRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

/**
 * 网关会话上下文
 * <p>全响应式令牌认证，依赖 {@link SessionRepository} 查库。</p>
 * <p> 不支持（网关为响应式环境，无 Servlet 请求上下文）。</p>
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 */
@Slf4j
@Component
public class ReactSessionCtx implements SessionCtx {
    @Resource
    private SessionRepository sessionRepository;

    @Override
    public Session currentSession() {
        throw new UnsupportedOperationException("网关服务不支持获取当前请求会话");
    }

    @Override
    public Session auth(Set<String> tokens) {
        throw new UnsupportedOperationException("网关服务请使用 authMono 响应式方法");
    }

    @Override
    public Mono<Session> authMono(Set<String> tokens) {
        if (Objects.isNull(tokens) || tokens.isEmpty())
            return Mono.empty();

        return Flux.fromIterable(tokens)
                .map(t -> t.replaceFirst("Bearer ", "").trim())
                .filter(t -> !t.isBlank())
                .flatMap(this::verifyAndFetch, 1)
                .next();
    }

    /**
     * 验签 → 查库，全响应式
     */
    private Mono<Session> verifyAndFetch(String token) {
        boolean valid = SessionTokenUtil.verify(token);
        if (!valid) {
            log.warn("令牌验签失败: {}", token);
            return Mono.empty();
        }

        return sessionRepository.findByTokenMono(token)
                .doOnNext(session -> log.debug("令牌认证成功: token={}, userid={}", session.getToken(), session.getUserid()));
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
