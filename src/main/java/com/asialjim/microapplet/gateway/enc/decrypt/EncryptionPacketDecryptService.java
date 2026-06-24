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

import com.asialjim.microapplet.session.MamsUserEncKeyResParam;
import com.asialjim.microapplet.session.MamsUserEncrypt;
import com.asialjim.microapplet.gateway.context.MediaTypeResCode;
import com.asialjim.microapplet.gateway.enc.Decrypter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 加密数据宝解密服务
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/9, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptionPacketDecryptService {
    private final Map<String, Decrypter> decrypterMap = new HashMap<>();
    private final Map<Class<?>, DecryptionWrapper<?>> wrapperMap = new HashMap<>();
    private final List<Decrypter> decrypters;
    private final List<DecryptionWrapper<?>> wrappers;

    public DecryptionPacket decrypt(
            EncryptionPacket<?> encryptionPacket,
            MamsUserEncKeyResParam encryptKey) {

        Decrypter decrypter = candidateDecrypter(encryptKey);
        byte[] decrypt = decrypter.decrypt(encryptionPacket, encryptKey);
        DecryptionWrapper<?> wrapper = candidateWrapper(encryptionPacket.decryptionPacketType());
        return wrapper.wrap(decrypt);
    }

    private DecryptionWrapper<?> candidateWrapper(Class<?> packetType) {
        return wrapperMap.computeIfAbsent(
                packetType,
                k -> wrappers.stream()
                        .filter(item -> item.decryptionType().isAssignableFrom(packetType))
                        .findFirst()
                        .orElseThrow(MediaTypeResCode.UnSupported::ex));
    }

    public Decrypter candidateDecrypter(MamsUserEncKeyResParam encryptKey) {
        return decrypterMap.computeIfAbsent(
                encryptKey.getEncryptType(),
                name -> decrypters.stream()
                        .filter(item -> item.support(name))
                        .findFirst()
                        .orElseThrow(MediaTypeResCode.EncryptUnSupport::ex));
    }
}
