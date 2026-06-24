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
import com.asialjim.microapplet.gateway.enc.decrypt.DecryptionPacket;
import org.apache.commons.lang3.ArrayUtils;
import tools.jackson.databind.JavaType;

/**
 * 二进制数据解密数据包
 * <pre>
 *      类型                    可用性
 *      application/json        ✅
 *      application/xml         ✅
 *      others                  ❌
 * </pre>
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/5, &nbsp;&nbsp; <em>version:1.0</em>
 */
public record ByteArrayDecryptionPacket(byte[] body) implements DecryptionPacket {
    @Override
    public String decryptionPacketBodyText() {
        if (ArrayUtils.isEmpty(body))
            return "@_EMPTY_BUFFER_@";
        try {
            JavaType javaType = JsonUtil.instance.constructType(String.class);
            return JsonUtil.instance.toBean(this.body, javaType);
        } catch (Throwable t) {
            return new String(this.body);
        }
    }
}