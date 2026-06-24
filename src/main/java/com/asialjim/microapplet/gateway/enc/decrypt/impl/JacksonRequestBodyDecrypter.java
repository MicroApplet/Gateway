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

package com.asialjim.microapplet.gateway.enc.decrypt.impl;

import com.asialjim.microapplet.gateway.enc.decrypt.EncryptionPacket;
import com.asialjim.microapplet.gateway.enc.decrypt.RequestBodyDecrypter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

/**
 * 基于 jackson 的加密请求体处理器
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/5, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
public abstract class JacksonRequestBodyDecrypter implements RequestBodyDecrypter<ByteArrayDecryptionPacket> {

    /**
     * jackson 处理器，用于从请求体中提取加密数据报文
     *
     * @return {@link ObjectMapper }
     * @since 2026/2/5
     */
    protected abstract ObjectMapper objectMapper();

    protected JsonNode extractFromRequestBody(byte[] inputStream) {
        return objectMapper().readTree(inputStream);
    }

    private static String extractTextValue(String field, JsonNode jsonNode) {
        if (Objects.isNull(jsonNode) || StringUtils.isBlank(field))
            return StringUtils.EMPTY;
        boolean hadField = jsonNode.hasNonNull(field);
        if (!hadField)
            return StringUtils.EMPTY;

        JsonNode node = jsonNode.get(field);
        if (!node.isValueNode())
            return StringUtils.EMPTY;

        return node.asString();
    }


    @Override
    public Mono<EncryptionPacket<ByteArrayDecryptionPacket>> extract(ServerHttpRequest request) {
        Map<String, String> headers = request.getHeaders().toSingleValueMap();
        MultiValueMap<String, String> parameters = request.getQueryParams();

        return DataBufferUtils.join(request.getBody())
                .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                .flatMap(dataBuffer ->
                        Mono.fromCallable(() -> {
                                    byte[] requestBody = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(requestBody);
                                    return extractFromRequestBody(requestBody);
                                })
                                .doFinally(s -> DataBufferUtils.release(dataBuffer))
                )
                .map(map ->
                        new EncryptionPacket<>(
                                request,
                                headers,
                                parameters,
                                extractTextValue(EncryptionPacket.ENCRYPT_FIELD, map),
                                extractTextValue(EncryptionPacket.SIGNATURE_FIELD, map),
                                ByteArrayDecryptionPacket.class
                        )
                );
    }

    @Override
    public ServerHttpRequest packet(ServerHttpRequest request, ByteArrayDecryptionPacket decryptedBody) {
        byte[] body = decryptedBody.body();
        return wrap(request, body);
    }
}