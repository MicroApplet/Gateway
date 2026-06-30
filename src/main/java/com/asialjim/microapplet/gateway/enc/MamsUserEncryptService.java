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

package com.asialjim.microapplet.gateway.enc;

import com.asialjim.microapplet.commons.chl.PlatformAppType;
import com.asialjim.microapplet.session.MamsUserEncKeyRepository;
import com.asialjim.microapplet.session.MamsUserEncKeyResParam;
import com.asialjim.microapplet.session.SessionResCode;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class MamsUserEncryptService {
    private final Map<String, SubAppUserEncryptAdaptor> adaptorMap = new HashMap<>();
    @Resource
    private MamsUserEncKeyRepository reactiveRedisMamsUserEncKeyRepository;

    @Lazy
    @Resource
    private List<SubAppUserEncryptAdaptor> adaptors;

    private SubAppUserEncryptAdaptor adaptorOf(PlatformAppType platformAppType) {
        String code = platformAppType.uniCode();
        return adaptorMap.computeIfAbsent(code,
                _ ->
                        adaptors.stream()
                                .filter(item -> item.support(platformAppType))
                                .findFirst()
                                .orElseThrow(SessionResCode.EmptyAppTypeUserEncrypter::ex));
    }


    public Mono<MamsUserEncKeyResParam> encryptOf(PlatformAppType platformAppType, String appid, String openid, String sessionKey, String version) {
        String subAppTypeCode = platformAppType.uniCode();
        return this.reactiveRedisMamsUserEncKeyRepository.getMono(subAppTypeCode, openid, version)
                .switchIfEmpty(Mono.defer(() -> {
                    SubAppUserEncryptAdaptor adaptor = adaptorOf(platformAppType);
                    return adaptor.encryptOf(appid, openid, sessionKey, version)
                            .flatMap(key -> {
                                //noinspection ConstantValue
                                if (Objects.nonNull(key) && StringUtils.isNotBlank(key.getUserKey()))
                                    return reactiveRedisMamsUserEncKeyRepository.setMono(subAppTypeCode, openid, version, key)
                                            .thenReturn(key);

                                return Mono.justOrEmpty(key);
                            });
                }));
    }
}