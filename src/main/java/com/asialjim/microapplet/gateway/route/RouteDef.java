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

package com.asialjim.microapplet.gateway.route;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@Data
public class RouteDef {
    private String id;

    /**
     * 路由名称
     */
    private String name;

    /**
     * 前缀类型
     */
    private int parts;

    public int parts() {
        return this.parts;
    }

    private boolean enableAuth;

    public boolean enableAuth() {
        return this.enableAuth;
    }

    private boolean enableReqDec;

    public boolean enableReqDec() {
        return this.enableReqDec;
    }

    private boolean enableResEnc;

    public boolean enableResEnc() {
        return this.enableResEnc;
    }


    private String prefix;
    /**
     * 基于 path 的路由匹配规则
     */
    private String path;

    /**
     * 转发服务名
     */
    private String service;

    private String desc;
    private Integer order;
    private Boolean fallback;

    public boolean getFallback() {
        return Optional.ofNullable(this.fallback).orElse(false);
    }

    public String prefix() {
        if (StringUtils.isBlank(prefix))
            return StringUtils.EMPTY;
        if (!prefix.startsWith("/"))
            prefix = "/" + prefix;
        if (prefix.endsWith("/"))
            prefix = prefix.substring(prefix.length() - 1);
        return prefix;
    }

    public String pathPattern() {
        String path = prefix() + path();
        if (path.endsWith("**"))
            return path;

        if (path.endsWith("/"))
            return path + "**";
        return path + "/**";
    }

    private String path() {
        if (StringUtils.isBlank(path))
            return StringUtils.EMPTY;
        path = path.trim();
        if (path.startsWith("/"))
            return path;
        return "/" + path;
    }
}