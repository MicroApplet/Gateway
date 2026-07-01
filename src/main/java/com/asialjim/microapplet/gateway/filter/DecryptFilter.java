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

import com.asialjim.microapplet.commons.standard.utils.JacksonUtil;
import com.asialjim.microapplet.session.MamsUserEncKeyResParam;
import com.asialjim.microapplet.session.SessionCtx;
import com.asialjim.microapplet.commons.chl.PlatformAppType;
import com.asialjim.microapplet.commons.standard.utils.JsonUtil;
import com.asialjim.microapplet.session.Session;
import com.asialjim.microapplet.gateway.context.AuthenticateResCode;
import com.asialjim.microapplet.gateway.enc.Decrypter;
import com.asialjim.microapplet.gateway.enc.MamsUserEncryptService;
import com.asialjim.microapplet.gateway.enc.decrypt.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static com.asialjim.microapplet.gateway.context.AuthenticateResCode.SignatureFailure;

/**
 * 请求体加密通讯解密组件
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/27, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Component
public class DecryptFilter implements GatewayFilter {
    public static final String NAME = "DecryptFilter";

    private final DecryptExtractorService decryptExtractorService;
    private final MamsUserEncryptService mamsUserEncryptService;
    private final EncryptionPacketDecryptService encryptionPacketDecryptService;

    public static class Config {
    }

    public DecryptFilter(DecryptExtractorService decryptExtractorService, MamsUserEncryptService mamsUserEncryptService, EncryptionPacketDecryptService encryptionPacketDecryptService) {
        this.decryptExtractorService = decryptExtractorService;
        this.mamsUserEncryptService = mamsUserEncryptService;
        this.encryptionPacketDecryptService = encryptionPacketDecryptService;
    }

    private static MultiValueMap<String, String> parseQueries(String msgFormat, String formData) {
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        if (StringUtils.isBlank(formData)) return result;

        if ("json".equals(msgFormat)){
            Map<String, String> map = JsonUtil.instance.toBean(formData, JacksonUtil.STRING_TYPE);
            map.forEach((k, v) -> {
                String key = URLDecoder.decode(k, StandardCharsets.UTF_8);
                String value = URLDecoder.decode(v, StandardCharsets.UTF_8);
                result.add(key, value);
            });

            return result;
        }

        if("x-www-urlencoded".equalsIgnoreCase(msgFormat)){
            //noinspection DuplicatedCode
            String[] pairs = StringUtils.split(formData, "&");
            for (String pair : pairs) {
                String[] kv = StringUtils.split(pair, "=");
                if (ArrayUtils.getLength(kv) != 2) continue;

                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                result.add(key, value);
            }
        }

        return result;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return encKey(exchange)
                .doOnNext(item -> {
                    if (log.isDebugEnabled()) log.debug("获取到用户加密通讯秘钥：{}", item);
                })
                // 秘钥缺失时明确报错，避免加密请求被静默放行或返回空响应
                .switchIfEmpty(Mono.error(AuthenticateResCode.EncryptKeyMiss::ex))
                .flatMap(key -> decrypt(exchange, key))
                .flatMap(chain::filter);
    }

    private Mono<ServerWebExchange> decrypt(ServerWebExchange exchange, MamsUserEncKeyResParam key) {
        Decrypter decrypter = encryptionPacketDecryptService.candidateDecrypter(key);
        if (log.isDebugEnabled()) log.debug("获取到解密器：{}", decrypter);

        return Mono.just(exchange.getRequest())
                .flatMap(req -> decryptRequest(exchange, key, req, decrypter))
                .map(item -> exchange.mutate().request(item).build());
    }

    private Mono<ServerHttpRequest> decryptRequest(ServerWebExchange exchange, MamsUserEncKeyResParam key, ServerHttpRequest req, Decrypter decrypter) {
        HttpHeaders headers = req.getHeaders();
        MultiValueMap<String, String> queries = req.getQueryParams();

        if (log.isDebugEnabled()) log.debug("原查询参数：{}", queries);
        if (queries.containsKey(EncryptionPacket.ENCRYPT_FIELD) && queries.containsKey(EncryptionPacket.SIGNATURE_FIELD))
            queries = decryptQueryParams(key, decrypter, queries);

        URI originalUri = req.getURI();
        if (log.isDebugEnabled()) log.debug("原URL: {}", originalUri);
        URI uri = UriComponentsBuilder.fromUri(originalUri).replaceQueryParams(queries).build(true).toUri();
        if (log.isDebugEnabled()) log.debug("目标URL: {}", uri);

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri);

        ServerHttpRequest urlReq = req.mutate().uri(uri).build();

        // ContentLength <= 0 表示没有请求体，无需请求体解密，原样放行
        long contentLength = headers.getContentLength();
        if (contentLength <= 0) return Mono.just(urlReq);

        // 缺少 Content-Type 时无法定位请求体解密器，原样放行，避免链路静默中断
        MediaType contentType = headers.getContentType();
        if (Objects.isNull(contentType)) return Mono.just(urlReq);

        return Mono.just(contentType)
                .doOnNext(item -> {
                    if (log.isDebugEnabled()) log.debug("请求体类型：{}", item);
                })
                .flatMap(decryptExtractorService::decrypterOfMono)
                .doOnNext(item -> {
                    if (log.isDebugEnabled()) log.debug("请求体解密器：{}", item);
                })
                .flatMap(bodyDecrypter -> decryptRequestBody(key, req, bodyDecrypter, urlReq))
                // 未匹配到请求体解密器时原样放行，避免链路静默中断
                .switchIfEmpty(Mono.just(urlReq));
    }

    private Mono<ServerHttpRequest> decryptRequestBody(MamsUserEncKeyResParam key, ServerHttpRequest req, RequestBodyDecrypter<?> bodyDecrypter, ServerHttpRequest urlReq) {
        return bodyDecrypter.extract(req)
                .doOnNext(item -> {
                    if (log.isDebugEnabled()) log.debug("请求体原文：{}", item);
                })
                .map(extract -> encryptionPacketDecryptService.decrypt(extract, key))
                .doOnNext(item -> {
                    if (log.isDebugEnabled()) log.debug("请求体明文：{}", item.decryptionPacketBodyText());
                })
                .map(item -> bodyDecrypter.packet(urlReq, item));
    }

    private static MultiValueMap<String, String> decryptQueryParams(MamsUserEncKeyResParam key, Decrypter decrypter, MultiValueMap<String, String> queries) {
        String msgFormat = queries.getFirst("messageFormat");
        String sign = queries.getFirst(EncryptionPacket.SIGNATURE_FIELD);
        String encrypt = queries.getFirst(EncryptionPacket.ENCRYPT_FIELD);
        // 明文
        String plaintext = decrypter.decrypt(encrypt, key.getUserKey(), key.getIv());
        // 生成签名
        String expectSignature = decrypter.sign(encrypt, key.getUserKey());

        // 签名验证
        if (!Strings.CI.equals(expectSignature,sign))
            // 验证失败
            SignatureFailure.thr();

        queries = parseQueries(msgFormat, plaintext);
        if (log.isDebugEnabled()) log.debug("解密后查询参数：{}", queries);
        return queries;
    }

    private Mono<MamsUserEncKeyResParam> encKey(ServerWebExchange exchange){
        Object o = exchange.getAttribute(SessionCtx.userSessionAttribute);
        if (Objects.isNull(o) || !(o instanceof Session session))
            throw AuthenticateResCode.EncryptMiss.ex();

        String openid = session.getOpenid();
        PlatformAppType subChannel = session.platformAppType();
        String appid = session.getAppid();
        String sessionKey = session.getSessionKey();

        HttpHeaders headers = exchange.getRequest().getHeaders();
        if (!headers.containsHeader("Version")) throw AuthenticateResCode.EncryptKeyMiss.ex("请求头缺失 Version");

        String version = headers.getFirst("Version");
        if (log.isDebugEnabled()) log.debug("用户加密通讯秘钥版本号: {}", version);
        if (StringUtils.isBlank(version)) AuthenticateResCode.EncryptKeyMiss.thr();

        return this.mamsUserEncryptService.encryptOf(subChannel, appid, openid, sessionKey, version);
    }
}