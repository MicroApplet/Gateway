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

/**
 * 网关前缀类型
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/10/23, &nbsp;&nbsp; <em>version:1.0</em>
 */
public enum Prefix {
    /** REST API 路由 /api/rest/* */
    rest,
    /** 开放接口路由 /api/open/* */
    open,
    /** 管理后台路由 /api/admin/* */
    admin,
    /** AI 对话路由 /api/ai/* */
    ai
}