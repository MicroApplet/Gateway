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

package com.asialjim.microapplet.gateway.enc.decrypt;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 请求体解密器
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/5, &nbsp;&nbsp; <em>version:1.0</em>
 */
public interface RequestBodyDecrypter<T extends DecryptionPacket> {

    /**
     * 本组件是否支持处理该请求
     *
     * @param contentType {@link MediaType contentType}
     * @return {@link Boolean }
     * @since 2026/2/5
     */
    default boolean supportType(MediaType contentType) {
        if (Objects.isNull(contentType))
            return false;

        MediaType supportType = contentType();
        if (Objects.isNull(supportType))
            return false;

        return supportType.getType().equals(contentType.getType())
                && supportType.getSubtype().equals(contentType.getSubtype());
    }

    MediaType contentType();


    /**
     * 提取报文
     *
     * @return {@link EncryptionPacket }
     * @since 2025/11/20
     */
    Mono<EncryptionPacket<T>> extract(ServerHttpRequest request);

    /**
     * 包装解密后的数据
     *
     * @param decryptedBody {@link T body}
     * @since 2025/11/20
     */
    ServerHttpRequest packet(ServerHttpRequest request, T decryptedBody);

    default ServerHttpRequest packet(ServerHttpRequest request, Object decryptedBody) {
        //noinspection unchecked
        return packet(request, (T) decryptedBody);
    }

    default ServerHttpRequestDecorator wrap(ServerHttpRequest request, byte[] body) {
        return new ServerHttpRequestDecorator(request) {

            @Override
            @SuppressWarnings("NullableProblems")
            public Flux<DataBuffer> getBody() {
                if (Objects.isNull(body))
                    return Flux.empty();

                DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(body);
                return Flux.just(buffer);
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.setContentLength(body.length);
                return headers;
            }
        };
    }
}