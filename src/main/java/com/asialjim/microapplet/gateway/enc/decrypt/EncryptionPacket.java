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

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;

import java.util.Map;

/**
 * 加密通讯报文数据包
 *
 * @param <T>                  对应的解密报文数据包类型
 * @param request              原请求对象
 * @param headers              原始请求头
 * @param parameters           原始请求参数
 * @param encrypt              原始加密报文体
 * @param signature            原始签名数据
 * @param decryptionPacketType 解密后的数据包类型
 * @author Asial Jim
 * @version 1.0
 * @since 2025/11/20, &nbsp;&nbsp; <em>version:1.0</em>
 */
public record EncryptionPacket<T extends DecryptionPacket>(
        ServerHttpRequest request,
        Map<String, String> headers,
        MultiValueMap<String, String> parameters,
        String encrypt,
        String signature,
        Class<T> decryptionPacketType) {

    public static final String ENCRYPT_FIELD = "params";
    public static final String SIGNATURE_FIELD = "sign";
}