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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 网关路由配置刷新监听器
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/6/22, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayRouteRefreshListener {
    private final ApplicationEventPublisher publisher;

    @EventListener(EnvironmentChangeEvent.class)
    public void onEnvironmentChange(EnvironmentChangeEvent event) {
        boolean routeChanged = event.getKeys().stream().anyMatch(this::isRouteConfigKey);
        if (!routeChanged) return;

        log.info("网关路由配置发生变化，刷新路由表：{}", event.getKeys());
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    private boolean isRouteConfigKey(String key) {
        return key.startsWith("gateway.routes")
                || "gateway.enable-dev".equals(key)
                || "gateway.enableDev".equals(key);
    }
}
