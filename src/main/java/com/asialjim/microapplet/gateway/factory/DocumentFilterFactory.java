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

import com.asialjim.microapplet.gateway.filter.DocumentFilter;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * 用户认证过滤器工厂
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/9/25, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Configuration
public class DocumentFilterFactory extends AbstractGatewayFilterFactory<DocumentFilterFactory.Config> {

    public DocumentFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        DocumentFilter filter = new DocumentFilter();
        String context = config.getContext();
        String docPath = config.getDocPath();
        docPath = StringUtils.startsWith(docPath,"/")?docPath: "/" + docPath;
        filter.setContext(context + docPath);
        return filter;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.emptyList();
    }

    @Data
    public static class Config {
        private String context;
        private String docPath;
    }

    @Override
    public String name() {
        return "DocumentFilter";
    }
}