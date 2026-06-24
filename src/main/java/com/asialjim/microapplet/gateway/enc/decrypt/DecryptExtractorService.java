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

import com.asialjim.microapplet.commons.standard.exception.BusinessException;
import com.asialjim.microapplet.gateway.context.MediaTypeResCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 加密的请求体报文解析器
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/5, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Component
@AllArgsConstructor
public class DecryptExtractorService {
    private final Map<MediaType, RequestBodyDecrypter<?>> decrypterMap = new HashMap<>();

    private final List<RequestBodyDecrypter<?>> decrypters;

    public Mono<RequestBodyDecrypter<?>> decrypterOfMono(MediaType contentType) {
        return Mono.just(decrypterOf(contentType));
    }

    public RequestBodyDecrypter<?> decrypterOf(MediaType contentType) {
        if (Objects.isNull(contentType)){
            log.error("请求体媒体类型为空");
            throw  MediaTypeResCode.ContentTypeMiss.ex();
        }

        RequestBodyDecrypter<?> decrypter = decrypterMap.get(contentType);
        if (Objects.nonNull(decrypter))
            return decrypter;

        decrypter = decrypters.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.supportType(contentType))
                .findAny()
                .orElseThrow(() -> {
                    log.error("不支持的媒体类型：{}", contentType);
                    return MediaTypeResCode.UnSupported.ex();
                });

        decrypterMap.put(contentType,decrypter);
        return decrypter;
    }
}