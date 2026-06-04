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

package com.asialjim.microapplet.gateway.config;

import com.alibaba.druid.util.StringUtils;
import com.asialjim.microapplet.gateway.route.RouteConfigProperty;
import com.asialjim.microapplet.gateway.route.RouteNode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
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
    public void destroy() throws Exception {
        log.info("NacosRouteDefinitionRepository正在销毁，清理路由缓存和相关资源...");
        clearRoutes();
        // 清除对applicationEventPublisher的引用，避免内存泄漏
        this.applicationEventPublisher = null;
        log.info("NacosRouteDefinitionRepository资源清理完成");
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.doOnNext(id -> routes.removeIf(item -> StringUtils.equals(id, item.getId())))
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
        return Flux.fromIterable(routes).doOnSubscribe(subscription -> log.debug("获取路由定义列表"));
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
            List<RouteNode> newRoutes = routeConfigProperty.getRoutes();
            log.info("开始加载新路由，共 {} 条", newRoutes.size());

            for (RouteNode route : newRoutes) {
                RouteDefinition definition = convertToRouteDefinition(routeJ, route);
                save(Mono.just(definition)).block(); // 使用block()确保同步添加
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
        FilterDefinition TraceFilter = new FilterDefinition();
        TraceFilter.setName("TraceFilter");
        filters.add(TraceFilter);

        FilterDefinition PageNotFoundFilter = new FilterDefinition();
        PageNotFoundFilter.setName("PageNotFoundFilter");
        filters.add(PageNotFoundFilter);

        definition.setFilters(filters);
        return definition;
    }

    /**
     * 将RouteNode转换为RouteDefinition
     */
    private RouteDefinition convertToRouteDefinition(StringJoiner routeJ, RouteNode routeNode) {
        RouteDefinition definition = new RouteDefinition();
        String name = routeNode.getName();
        String prefix = routeNode.getPrefix().name();
        String path = "/api/" + prefix + "/" + routeNode.getPath();
        String pathPattern = path + "/**";
        boolean enableAuth = routeNode.enableAuth();
        String service = routeNode.getService();
        String remark = routeNode.getRemark();
        URI uri = URI.create("lb://" + service);

        StringBuilder sb = new StringBuilder();
        sb.append("\r\n\t").append("路由:").append("\t").append(name);
        sb.append("\r\n\t").append("规则:").append("\t").append(pathPattern);
        sb.append("\r\n\t").append("转发:").append("\t").append(uri);
        sb.append("\r\n\t").append("备注:").append("\t").append(remark);

        definition.setId(name);
        definition.setUri(uri);

        // 设置Predicate
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        predicate.addArg("pattern", pathPattern);
        definition.setPredicates(Collections.singletonList(predicate));


        StringJoiner filterJ = new StringJoiner("; ");

        // 设置Filters
        final List<FilterDefinition> filters = new ArrayList<>();
        FilterDefinition TraceFilter = new FilterDefinition();
        TraceFilter.setName("TraceFilter");
        filters.add(TraceFilter);
        filterJ.add(TraceFilter.getName() + "[链路追踪]");

        // StripPrefix Filter
        FilterDefinition stripPrefixFilter = new FilterDefinition();
        stripPrefixFilter.setName("StripPrefix");
        stripPrefixFilter.addArg("parts", "2");
        filters.add(stripPrefixFilter);
        filterJ.add(stripPrefixFilter.getName() + "=2");

        // RewritePath Filter
        FilterDefinition rewritePathFilter = new FilterDefinition();
        rewritePathFilter.setName("RewritePath");
        rewritePathFilter.addArg("regexp", "/" + routeNode.getPath() + "(?<segment>.*)");
        rewritePathFilter.addArg("replacement", "${segment}");
        filters.add(rewritePathFilter);
        filterJ.add("RewritePath=" + pathPattern + "-> /" + routeNode.getPath() + "/**");

        // 添加全局过滤器（通过配置方式）
        if (enableAuth) {
            FilterDefinition documentFilter = new FilterDefinition();
            documentFilter.setName("DocumentFilter");
            documentFilter.addArg("context", path);
            documentFilter.addArg("docPath", "/static/doc/");
            filters.add(documentFilter);
            filterJ.add("DocumentFilter[文档过滤器]");

            FilterDefinition authFilter = new FilterDefinition();
            authFilter.setName("AuthFilter");
            filters.add(authFilter);
            filterJ.add("AuthFilter[认证过滤器]");
        }

        definition.setFilters(filters);
        sb.append("\r\n\t").append("拦截:").append("\t").append(filterJ);
        routeJ.add(sb);
        return definition;
    }
}