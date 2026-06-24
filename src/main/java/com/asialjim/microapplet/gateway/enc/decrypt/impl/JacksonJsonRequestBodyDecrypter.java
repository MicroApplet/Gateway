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
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * 基于Jackson的json请求体解包器
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/5, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Component
public class JacksonJsonRequestBodyDecrypter extends JacksonRequestBodyDecrypter {
    private final ObjectMapper objectMapper;

    public JacksonJsonRequestBodyDecrypter(@Nullable JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter) {
        this.objectMapper = Optional.ofNullable(jacksonJsonHttpMessageConverter)
                .map(AbstractJacksonHttpMessageConverter::getMapper)
                .orElseGet(JsonUtil.instance::objectMapper);
    }

    /**
     * jackson 处理器，用于从请求体中提取加密数据报文
     *
     * @return {@link ObjectMapper }
     * @since 2026/2/5
     */
    @Override
    protected ObjectMapper objectMapper() {
        return this.objectMapper;
    }


    @Override
    public MediaType contentType() {
        return MediaType.APPLICATION_JSON;
    }


}