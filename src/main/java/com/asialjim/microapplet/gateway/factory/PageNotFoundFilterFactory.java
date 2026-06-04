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

package com.asialjim.microapplet.gateway.factory;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * 404 拦截工厂
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/9/25, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Configuration
public class PageNotFoundFilterFactory extends AbstractGatewayFilterFactory<PageNotFoundFilterFactory.Config> {

    private final GatewayFilter global404Filter;

    public PageNotFoundFilterFactory(GatewayFilter global404Filter) {
        super(Config.class);
        this.global404Filter = global404Filter;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return global404Filter;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.emptyList();
    }

    public static class Config {
        // 可以添加配置参数
    }

    @Override
    public String name() {
        return "PageNotFoundFilter";
    }
}