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

import com.asialjim.microapplet.gateway.filter.*;
import jakarta.annotation.Resource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于 nacos 的动态路由策略仓库
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/9/25, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Configuration
public class NacosRouteDefinitionRepository implements RouteDefinitionRepository, ApplicationEventPublisherAware, DisposableBean {
    private final List<RouteDefinition> routes = new CopyOnWriteArrayList<>();

    @Setter
    private ApplicationEventPublisher applicationEventPublisher;

    @Resource
    private WebFluxProperties webFluxProperties;

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.doOnNext(routes::add)
                .doOnNext(item -> log.info("添加路由：{}", item))
                .then();
    }
    
    /**
     * 应用关闭时清理资源，防止内存泄漏
     */
    @Override
    public void destroy() {
        log.info("NacosRouteDefinitionRepository正在销毁，清理路由缓存和相关资源...");
        clearRoutes();
        // 清除对applicationEventPublisher的引用，避免内存泄漏
        this.applicationEventPublisher = null;
        log.info("NacosRouteDefinitionRepository资源清理完成");
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.doOnNext(id -> routes.removeIf(item -> id.equals(item.getId()) ))
                .doOnNext(id -> log.info("删除路由：{}", id))
                .then();
    }
    
    /**
     * 清空所有路由，用于应用关闭时清理资源
     */
    private void clearRoutes() {
            // 1. 使用同步方式清空现有路由
            log.info("清空现有路由...");
            List<String> routeIdsToDelete = new ArrayList<>();
            // 先收集所有路由ID
            routes.forEach(route -> routeIdsToDelete.add(route.getId()));
            // 然后批量删除
            for (String routeId : routeIdsToDelete) {
                delete(Mono.just(routeId)).block(); // 使用block()确保同步删除
            }
            log.info("已清空 {} 条路由", routeIdsToDelete.size());
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.fromIterable(routes);
    }


    /**
     * 刷新路由配置
     */
    public void refreshRoutes(RouteConfigProperty routeConfigProperty) {
        log.info("开始刷新路由配置...");

        if (Objects.isNull(routeConfigProperty)) {
            log.warn("路由配置为空，跳过路由刷新");
            return;
        }

        try {
            // 1. 使用同步方式清空现有路由
            clearRoutes();

            // 2. 同步添加新路由
            final StringJoiner routeJ = new StringJoiner("\r\n\t————————————");
            List<RouteDef> newRoutes = routeConfigProperty.getRoutes();
            log.info("开始加载新路由，共 {} 条", newRoutes.size());

            for (RouteDef route : newRoutes) {
                RouteDefinition definition = convertToRouteDefinition(routeJ, route);
                save(Mono.just(definition)).block();
            }

            // 添加404路由
            RouteDefinition route404 = route404();
            save(Mono.just(route404)).block();

            log.info("加载路由表完成:\n{}", routeJ);

            // 3. 确保applicationEventPublisher不为空
            if (applicationEventPublisher == null) {
                log.error("ApplicationEventPublisher未注入，无法发布路由刷新事件");
                return;
            }

            // 4. 发布刷新事件
            log.info("发布路由刷新事件...");
            applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
            log.info("路由刷新完成，当前路由数量: {}", this.routes.size());
        } catch (Exception e) {
            log.error("路由刷新过程中发生错误: {}", e.getMessage(), e);
        }
    }

    private RouteDefinition route404() {
        RouteDefinition definition = new RouteDefinition();
        definition.setId("PAGE404");

        final PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        predicate.addArg("pattern", "/**");
        definition.setPredicates(Collections.singletonList(predicate));
        definition.setUri(URI.create("https://404.asialjim.cn/"));

        // 设置Filters
        List<FilterDefinition> filters = new ArrayList<>();
        FilterDefinition traceFilter = new FilterDefinition();
        traceFilter.setName(TraceFilter.name);
        filters.add(traceFilter);

        FilterDefinition PageNotFoundFilter = new FilterDefinition();
        PageNotFoundFilter.setName(CustomerNotFoundFilter.name);
        filters.add(PageNotFoundFilter);

        definition.setFilters(filters);
        return definition;
    }

    /**
     * 将RouteNode转换为RouteDefinition
     */
    private RouteDefinition convertToRouteDefinition(StringJoiner routeJ, RouteDef item) {
        RouteDefinition def = new RouteDefinition();
        String name = item.getName();
        String service = item.getService();
        if (StringUtils.isBlank(service)) {
            log.error("Service URI is blank for route: {}, skipping", name);
            return null; // Or throw a specific exception depending on requirements
        }

        if (!service.contains("://"))
            service = "lb://" + service;
        URI uri = URI.create(service);
        String pathPattern = item.pathPattern();

        int parts = item.parts();
        boolean auth = item.enableAuth();
        boolean reqEnc = item.enableReqDec();
        String basePath = this.webFluxProperties.getBasePath();
        if (StringUtils.isNotBlank(basePath) && pathPattern.startsWith(basePath)) {
            pathPattern = pathPattern.replaceFirst(basePath, "");
        }

        StringJoiner filterJ = new StringJoiner("; ");
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n\t").append("路由:").append("\t").append(name);
        sb.append("\r\n\t").append("规则:").append("\t").append(pathPattern);
        sb.append("\r\n\t").append("转发:").append("\t").append(uri);
        sb.append("\r\n\t").append("备注:").append("\t").append(item.getDesc());


        def.setId(name);
        def.setUri(uri);

        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        predicate.addArg("pattern", pathPattern);
        filterJ.add("Path" + "[路由转发], " + pathPattern);
        def.setPredicates(Collections.singletonList(predicate));

        // 编织过滤器
        List<FilterDefinition> filters = parseFilters(filterJ,pathPattern, item.getFallback(), parts, auth, reqEnc);

        def.setFilters(filters);
        def.setOrder(Optional.ofNullable(item.getOrder()).orElse(0));
        sb.append("\r\n\t").append("拦截:").append("\t").append(filterJ);
        routeJ.add(sb);
        return def;
    }

    private static List<FilterDefinition> parseFilters(StringJoiner filterJ,String path, boolean fallback, int parts,
                                                       boolean auth,
                                                       boolean reqEnc) {

        final List<FilterDefinition> filters = new ArrayList<>();

        // 全局异常处理
        FilterDefinition ex = new FilterDefinition();
        ex.setName(GlobalExceptionFilter.name);
        filters.add(ex);
        filterJ.add(GlobalExceptionFilter.name + "[全局异常处理]");

        // 链路追踪
        FilterDefinition trace = new FilterDefinition();
        trace.setName(TraceFilter.name);
        filters.add(trace);
        filterJ.add(TraceFilter.name + "[链路追踪]");

        // 路由转发
        FilterDefinition route = new FilterDefinition();
        route.setName(RouteFilter.name);
        filters.add(route);
        filterJ.add(RouteFilter.name + "[路由转发]");

        // 路由重写
        FilterDefinition strip = new FilterDefinition();
        strip.setName("StripPrefix");
        strip.addArg("parts", String.valueOf(parts));
        filters.add(strip);
        filterJ.add("RewritePath" + "[路由截断], " + parts);

       /*

        // StripPrefix Filter
        FilterDefinition stripPrefixFilter = new FilterDefinition();
        stripPrefixFilter.setName("StripPrefix");
        stripPrefixFilter.addArg("parts", "2");
        filters.add(stripPrefixFilter);
        */

        /*
        path = path.startsWith("/")?path.substring(1):path;
        String[] split = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts; i++) {
            if (StringUtils.isNotBlank(split[i]))
                sb.append("/").append(split[i]);
        }

        // RewritePath Filter
        FilterDefinition rewritePathFilter = new FilterDefinition();
        rewritePathFilter.setName("RewritePath");
        rewritePathFilter.addArg("regexp", sb + "(?<segment>.*)");
        rewritePathFilter.addArg("replacement", "${segment}");
        filterJ.add("RewritePath" + "[重写路由], " + sb + "(?<segment>.*)");
        filters.add(rewritePathFilter);
        */

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
                filterJ.add(AuthFilter.NAME + "[用户认证]");
            }
            // 不需要用户身份认证
            else {
                FilterDefinition annoFilter = new FilterDefinition();
                annoFilter.setName(AnnoFilter.NAME);
                filters.add(annoFilter);
                filterJ.add(AuthFilter.NAME + "[不启用用户认证]");
            }

            // 需要请求体报文解密
            if (reqEnc) {
                FilterDefinition reqEncFilter = new FilterDefinition();
                reqEncFilter.setName(DecryptFilter.NAME);
                filters.add(reqEncFilter);
                filterJ.add(AuthFilter.NAME + "[请求体解密]");
            }
        }

        return filters;
    }

}