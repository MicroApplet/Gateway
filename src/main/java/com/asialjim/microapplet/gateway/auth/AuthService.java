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

package com.asialjim.microapplet.gateway.auth;

import com.asialjim.microapplet.common.cons.Headers;
import com.asialjim.microapplet.common.context.Res;
import com.asialjim.microapplet.common.security.MamsSession;
import com.asialjim.microapplet.common.utils.JsonUtil;
import com.asialjim.microapplet.common.utils.MamsTokenUtil;
import com.asialjim.microapplet.gateway.cloud.AuthServiceLoadBalancerConfig;
import com.asialjim.microapplet.gateway.config.AuthServerProperty;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;

/**
 * 认证服务
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/12/2, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Service
public class AuthService {
    @Resource
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    @Resource
    private WebClient.Builder webClientBuilder;
    @Resource
    private AuthServerProperty authServerProperty;

    public Mono<MamsSession> validateToken(String token, String traceid) {

        return Mono.defer(() -> {
            // 验证令牌
            if (!MamsTokenUtil.verify(token)) {
                return Mono.error(Res.UserAuthFailure401Thr.ex(Collections.singletonList("非法令牌")));
            }

            String key = SessionCacheName.Name.userSessionByToken + "::" + token;
            // 查询会话信息（假设是反应式的 Redis 客户端）
            //noinspection ReactiveStreamsNullableInLambdaInTransform
            return reactiveRedisTemplate.opsForValue()
                    .get(key)
                    .map(o -> Objects.nonNull(o) && o instanceof MamsSession mamsSession ? mamsSession : null)
                    .flatMap((Function<MamsSession, Mono<MamsSession>>) session -> {
                        if (Objects.nonNull(session)) {
                            if (session.isExpired())
                                Res.UserAuthFailure401Thr.thr(Collections.singletonList("非法令牌"));
                            // 发布用户会话事件
                            return reactiveRedisTemplate.convertAndSend(Headers.CURRENT_SESSION, token)
                                    .doOnSuccess(session1 -> log.info("会话:{} 访问事件发布", token))
                                    .thenReturn(session);
                        }
                        System.err.println("缓存未空令牌");
                        return Mono.empty();
                    })
                    .switchIfEmpty(
                            webClientBuilder.build().get()
                                    .uri(authServerProperty.authUrl(token))
                                    .header(Headers.CLIENT_TYPE, Headers.CLOUD_CLIENT)
                                    .header(Headers.SessionId, "Auth")
                                    .header(Headers.TraceId, traceid)
                                    .header(Headers.TRACE_ID, traceid)
                                    .retrieve()
                                    .onStatus(HttpStatusCode::isError, AuthServiceLoadBalancerConfig.rsExFunction())
                                    .toEntity(String.class)
                                    .mapNotNull(HttpEntity::getBody)
                                    .mapNotNull(s -> JsonUtil.instance.toBean(s, MamsSession.class))
                    );
        }).cache(Duration.ofSeconds(5));
    }
}