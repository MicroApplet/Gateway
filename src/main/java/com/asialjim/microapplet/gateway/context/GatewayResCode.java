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

package com.asialjim.microapplet.gateway.context;

import com.asialjim.microapplet.commons.standard.context.ResCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 网关类型响应代码
 * 代码范围： 500 < code < 600
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/5, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Getter
@AllArgsConstructor
public enum GatewayResCode implements ResCode {
    ServiceNotFound(200,false,"503","服务未找到"),
    OK(200, false, "0", "OK");

    private final int status;
    private final boolean thr;
    private final String code;
    private final String msg;

    @Override
    public boolean isSuccess() {
        return !thr;
    }
}