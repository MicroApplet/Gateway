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

package com.asialjim.microapplet.gateway.enc.impl;

import com.asialjim.microapplet.commons.chl.PlatformAppType;
import com.asialjim.microapplet.commons.chl.SupportPlatformAppType;
import com.asialjim.microapplet.gateway.enc.SubAppUserEncryptAdaptor;
import com.asialjim.microapplet.session.MamsUserEncKeyResParam;
import com.asialjim.microapplet.session.MamsUserEncrypt;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;

/**
 * 抖音用户会话密钥适配器
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/3/10, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Lazy
@Slf4j
@Component
@AllArgsConstructor
public class TiktokUserEncryptAdaptor implements SubAppUserEncryptAdaptor {


    @Override
    public PlatformAppType subAppType() {
        return SupportPlatformAppType.TikTokApplet;
    }

    @Override
    public Mono<MamsUserEncKeyResParam> encryptOf(String appid, String openid, String sessionKey, String version) {
        return Mono.empty();
    }
}