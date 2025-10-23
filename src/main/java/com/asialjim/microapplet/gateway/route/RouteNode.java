/*
 *    Copyright 2014-2025 <a href="mailto:asialjim@qq.com">Asial Jim</a>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.asialjim.microapplet.gateway.route;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

/**
 * 动态路由节点
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/9/24, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Data
@Accessors(chain = true)
public class RouteNode implements Serializable {
    @Serial
    private static final long serialVersionUID = -8047150685067998552L;

    /**
     * 路由名称
     */
    private String name;

    private Prefix prefix;

    /**
     * 匹配规则, 仅基于path
     */
    private String path;

    /**
     * 是否需要登录
     */
    @JsonAlias("enable-auth")
    private Boolean enableAuth;
    /**
     * 路由服务
     */
    private String service;
    private Boolean http;

    private String remark;


    public Prefix getPrefix() {
        return Optional.ofNullable(this.prefix).orElse(Prefix.rest);
    }

    public String getPath() {
        return StringUtils.startsWith(path, "/") ? path.substring(1) : path;
    }


    public boolean http(){
        return  Optional.ofNullable(this.http).orElse(false);
    }


    public boolean enableAuth() {
        return Optional.ofNullable(this.enableAuth).orElse(true);
    }
}