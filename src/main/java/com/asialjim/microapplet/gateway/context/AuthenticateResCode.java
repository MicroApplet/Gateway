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
 * 用户身份凭证与加密通讯相关错误码
 * 代码范围： 400 < code < 404
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/5, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Getter
@AllArgsConstructor
public enum AuthenticateResCode implements ResCode {
    EncryptMiss(401,true,"401","用户加密通讯秘钥失效或不存在"),
    EncryptKeyMiss(401,true,"401","用户加密通讯秘钥版本号不存在"),
    SignatureFailure(401,true,"401","用户加密通讯签名验证失败"),
    Failure(401,true,"401","用户未登录或登录已过期"),
    TokenMiss(401,true,"401","用户身份令牌为空"),
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