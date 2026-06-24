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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 基于Jackson的json请求体解包器
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/5, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Component
public class UrlEncodedRequestBodyDecrypter implements RequestBodyDecrypter<MapDecryptionPacket> {


    @Override
    public MediaType contentType() {
        return MediaType.APPLICATION_FORM_URLENCODED;
    }

    /**
     * 提取报文
     *
     * @return {@link EncryptionPacket }
     * @since 2025/11/20
     */
    @Override
    public Mono<EncryptionPacket<MapDecryptionPacket>> extract(ServerHttpRequest request) {
        Map<String, String> headers = request.getHeaders().toSingleValueMap();
        MultiValueMap<String, String> parameters = request.getQueryParams();

        return DataBufferUtils.join(request.getBody())
                .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                .flatMap(dataBuffer ->
                                Mono.using(
                                        () -> dataBuffer,
                                        buffer -> {
                                            byte[] requestBody = new byte[buffer.readableByteCount()];
                                            buffer.read(requestBody);
                                            String bodyStr = new String(requestBody, StandardCharsets.UTF_8);
                                            return Mono.just(parseFormData(bodyStr));
                                        },
                                        DataBufferUtils::release
                                )
                )
                .map(map ->
                        new EncryptionPacket<>(
                                request,
                                headers,
                                parameters,
                                map.getFirst(EncryptionPacket.ENCRYPT_FIELD),
                                map.getFirst(EncryptionPacket.SIGNATURE_FIELD),
                                MapDecryptionPacket.class
                        )
                );
    }

    @Override
    public ServerHttpRequest packet(ServerHttpRequest request, MapDecryptionPacket decryptedBody) {
        Map<String, String[]> params = decryptedBody.params();
        StringJoiner builder = new StringJoiner("&");
        params.forEach((k, v) -> {
            for (String value : v) {
               /*
                String key = HtmlUtils.htmlEscape(URLEncoder.encode(k, StandardCharsets.UTF_8));
                String valueStr = HtmlUtils.htmlEscape(URLEncoder.encode(value, StandardCharsets.UTF_8));
                */

                String key = URLEncoder.encode(HtmlUtils.htmlEscape(k), StandardCharsets.UTF_8);
                String valueStr = URLEncoder.encode(HtmlUtils.htmlEscape(value), StandardCharsets.UTF_8);

                //String key = URLEncoder.encode(k, StandardCharsets.UTF_8);
                //String valueStr = URLEncoder.encode(value, StandardCharsets.UTF_8);
                builder.add(key + "=" + valueStr);
            }
        });
        String bodyStr = builder.toString();


        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        return wrap(request, body);
    }

    private static MultiValueMap<String, String> parseFormData(String formData) {
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        if (StringUtils.isBlank(formData)) return result;

        String[] pairs = StringUtils.split(formData, "&");
        for (String pair : pairs) {
            String[] kv = StringUtils.split(pair, "=");
            if (ArrayUtils.getLength(kv) != 2) continue;

            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            result.add(key, value);
        }

        return result;
    }
}