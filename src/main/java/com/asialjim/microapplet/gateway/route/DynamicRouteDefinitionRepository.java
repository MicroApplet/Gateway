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

import com.asialjim.microapplet.gateway.filter.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DynamicRouteDefinitionRepository implements RouteDefinitionRepository {
    private final static Logger log = LoggerFactory.getLogger(DynamicRouteDefinitionRepository.class);
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<RouteDefinition> routes = new CopyOnWriteArrayList<>();

    private final MamsRouteDefinitionRepository microBankRouteDefinitionRepository;
    private final WebFluxProperties webFluxProperties;

    public DynamicRouteDefinitionRepository(MamsRouteDefinitionRepository microBankRouteDefinitionRepository,
                                            WebFluxProperties webFluxProperties) {

        this.microBankRouteDefinitionRepository = microBankRouteDefinitionRepository;
        this.webFluxProperties = webFluxProperties;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Flux<RouteDefinition> getRouteDefinitions() {
        // 从动态路由表中获取所有路由定义，而不是从配置文件中读取，也不是用 SpringCloudGateway的内置路由定义提供者
        //noinspection DataFlowIssue,ConstantValue
        return Flux.fromIterable(this.microBankRouteDefinitionRepository.allRoutes())
                .map(this::trans2Route)
                .filter(Objects::nonNull)
                .doOnSubscribe(s -> log.debug("获取动态路由表"));
    }

    private RouteDefinition trans2Route(RouteDef item) {
        RouteDefinition def = new RouteDefinition();
        String name = item.getName();
        String service = item.getService();
        if (StringUtils.isBlank(service)) {
            log.error("Service URI is blank for route: {}, skipping", name);
            return null; // Or throw a specific exception depending on requirements
        }
        URI uri = URI.create(service);
        String pathPattern = item.pathPattern();

        int parts = item.parts();
        boolean auth = item.enableAuth();
        boolean reqEnc = item.enableReqDec();
        String basePath = this.webFluxProperties.getBasePath();
        if (StringUtils.isNotBlank(basePath) && pathPattern.startsWith(basePath)) {
            pathPattern = pathPattern.replaceFirst(basePath, "");
        }

        def.setId(name);
        def.setUri(uri);

        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        predicate.addArg("pattern", pathPattern);
        def.setPredicates(Collections.singletonList(predicate));

        // 编织过滤器
        List<FilterDefinition> filters = parseFilters(item.getFallback(),parts, auth, reqEnc);

        def.setFilters(filters);
        def.setOrder(Optional.ofNullable(item.getOrder()).orElse(0));
        return def;
    }

    private static List<FilterDefinition> parseFilters(boolean fallback, int parts,
                                                       boolean auth,
                                                       boolean reqEnc) {

        final List<FilterDefinition> filters = new ArrayList<>();

        // 全局异常处理
        FilterDefinition ex = new FilterDefinition();
        ex.setName(GlobalExceptionFilter.name);
        filters.add(ex);

        // 链路追踪
        FilterDefinition trace = new FilterDefinition();
        trace.setName(TraceFilter.name);
        filters.add(trace);

        // 路由转发
        FilterDefinition route = new FilterDefinition();
        route.setName(RouteFilter.name);
        filters.add(route);

        // 路由重写
        FilterDefinition strip = new FilterDefinition();
        strip.setName("StripPrefix");
        strip.addArg("parts", String.valueOf(parts));
        filters.add(strip);

        if (fallback) {
            // 资源未找到
            FilterDefinition notFound = new FilterDefinition();
            notFound.setName(CustomerNotFoundFilter.name);
            filters.add(notFound);
        } else {

            // 需要用户身份认证
            if (auth) {
                FilterDefinition authFilter = new FilterDefinition();
                authFilter.setName(AuthFilter.NAME);
                filters.add(authFilter);
            }
            // 不需要用户身份认证
            else {
                FilterDefinition annoFilter = new FilterDefinition();
                annoFilter.setName(AnnoFilter.NAME);
                filters.add(annoFilter);
            }

            // 需要请求体报文解密
            if (reqEnc) {
                FilterDefinition reqEncFilter = new FilterDefinition();
                reqEncFilter.setName(DecryptFilter.NAME);
                filters.add(reqEncFilter);
            }
        }

        return filters;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.doOnNext(routes::add)
                .doOnNext(item -> log.debug("添加路由：{}", item))
                .then();
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.doOnNext(id -> routes.removeIf(item -> id.equals(item.getId())))
                .doOnNext(item -> log.debug("删除路由：{}", item)).then();
    }
}