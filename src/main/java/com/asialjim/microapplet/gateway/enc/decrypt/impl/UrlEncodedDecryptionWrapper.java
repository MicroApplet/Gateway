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

import com.asialjim.microapplet.commons.standard.utils.JsonUtil;
import com.asialjim.microapplet.gateway.enc.decrypt.DecryptionWrapper;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * urlencoded明文通讯报文包装器
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/9, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Component
public class UrlEncodedDecryptionWrapper implements DecryptionWrapper<MapDecryptionPacket> {
    private final ObjectMapper objectMapper;

    public UrlEncodedDecryptionWrapper(@Nullable JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter) {
        objectMapper = Optional.ofNullable(jacksonJsonHttpMessageConverter)
                .map(AbstractJacksonHttpMessageConverter::getMapper)
                .orElseGet(JsonUtil.instance::objectMapper);
    }


    @Override
    public Class<MapDecryptionPacket> decryptionType() {
        return MapDecryptionPacket.class;
    }

    @Override
    @SneakyThrows
    public MapDecryptionPacket wrap(byte[] source) {
        Map<String, String[]> params = new HashMap<>();
        JavaType javaType = this.objectMapper.getTypeFactory().constructParametricType(HashMap.class, String.class, String.class);
        Map<String, String> map = this.objectMapper.readValue(source, javaType);
        if (Objects.nonNull(map))
            map.forEach((k, v) -> params.put(k, new String[]{v}));
        return new MapDecryptionPacket(params);
    }
}