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

package com.asialjim.microapplet.gateway.infrasturcture.repository;

import com.asialjim.microapplet.gateway.route.MamsRouteDefinitionRepository;
import com.asialjim.microapplet.gateway.route.RouteConfigProperty;
import com.asialjim.microapplet.gateway.route.RouteDef;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@AllArgsConstructor
public class RouteDefinitionRepositoryImpl implements MamsRouteDefinitionRepository {
    private final DiscoveryClient discoveryClient;

    @Resource
    private RouteConfigProperty routeConfigProperty;

    @Override
    public List<RouteDef> allRoutes() {
        List<RouteDef> routes = this.routeConfigProperty.getRoutes();
        List<RouteDef> res = new ArrayList<>(routes);

        if (Boolean.TRUE.equals(this.routeConfigProperty.getEnableDev())) {
            final List<String> services = discoveryClient.getServices();
            if (CollectionUtils.isNotEmpty(services)) {
                for (String service : services) {
                    if (log.isTraceEnabled())
                        log.trace("MAMS入口网关服务发现：{}", service);

                    res.add(createDocumentOfService(service));
                    res.add(createDevOfService(service));
                }
            }
        }

        return res;
    }


    private static RouteDef createDevOfService(String service) {
        RouteDef def = new RouteDef();
        def.setId("DevOf" + service);
        def.setName("DevOf" + service);
        def.setPrefix("/api/dev");
        def.setParts(3);

        def.setEnableAuth(true);
        def.setEnableReqDec(false);
        def.setEnableResEnc(false);

        def.setPath("/" + service + "/**");
        def.setService("lb://" + service);
        def.setDesc(service + "DEBUG" + service);
        return def;
    }

    private static RouteDef createDocumentOfService(String service) {
        RouteDef def = new RouteDef();
        def.setId("DocumentOf" + service);
        def.setName("DocumentOf" + service);
        def.setPrefix("/api/doc");
        def.setParts(3);

        def.setEnableAuth(false);
        def.setEnableReqDec(false);
        def.setEnableResEnc(false);

        def.setPath("/" + service + "/**");
        def.setService("lb://" + service);
        def.setDesc(service + "Doc" + service);
        return def;
    }
}