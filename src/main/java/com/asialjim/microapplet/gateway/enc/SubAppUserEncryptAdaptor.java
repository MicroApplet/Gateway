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
import com.asialjim.microapplet.session.MamsUserEncKeyResParam;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

public interface SubAppUserEncryptAdaptor {
    PlatformAppType subAppType();

    default boolean support(PlatformAppType platformAppType){
        if (Objects.isNull(platformAppType))
            return false;
        return Optional.ofNullable(subAppType())
                .map(PlatformAppType::uniCode)
                .map(item -> item.equals(platformAppType.uniCode()))
                .orElse(false);
    }

   Mono<MamsUserEncKeyResParam> encryptOf(String appid, String openid, String sessionKey, String version);
}
