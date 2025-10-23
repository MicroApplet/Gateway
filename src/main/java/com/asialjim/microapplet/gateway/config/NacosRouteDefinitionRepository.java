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
import com.asialjim.microapplet.common.utils.JsonUtil;
import com.asialjim.microapplet.gateway.route.RouteConfigProperty;
import com.asialjim.microapplet.gateway.route.RouteNode;
import jakarta.annotation.Resource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

/**
 * 基于 nacos 的动态路由策略仓库
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/9/25, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@RefreshScope
@Configuration
public class NacosRouteDefinitionRepository implements RouteDefinitionRepository, ApplicationEventPublisherAware {
    private final List<RouteDefinition> routes = new Vector<>();
    @Resource
    private RouteConfigProperty routeConfigProperty;
    @Setter
    private ApplicationEventPublisher applicationEventPublisher;

    private boolean initialized = false;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {
        if (initialized)
            return;
        log.info("应用准备就绪，开始初始化路由配置...");
        refreshRoutes(this.routeConfigProperty);
        initialized = true;
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.doOnNext(routes::add).doOnNext(item -> log.info("添加路由：{}", item.getId())).then();
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.doOnNext(id -> routes.removeIf(item -> StringUtils.equals(id, item.getId())))
                .doOnNext(id -> log.info("删除路由：{}", id))
                .then();
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

        // 清空现有路由
        this.routes.clear();
        final StringJoiner routeJ = new StringJoiner("\r\n\t————————————");
        List<RouteNode> routes = routeConfigProperty.getRoutes();

        for (RouteNode route : routes) {
            RouteDefinition definition = convertToRouteDefinition(routeJ, route);
            this.routes.add(definition);
        }
        this.routes.add(route404());

        //routes.stream().map(this::convertToRouteDefinition).forEach(item -> addRoute(routeJ,item));
        //addRoute(routeJ, route404());
        log.info("加载路由表{}", routeJ);

        // 发布路由刷新事件
        Optional.ofNullable(this.applicationEventPublisher)
                .ifPresent(publisher -> {
                    publisher.publishEvent(new RefreshRoutesEvent(this));
                    log.info("路由刷新完成，当前路由数量: {}", this.routes.size());
                });
    }

    /*private void addRoute(StringJoiner routeLog, RouteDefinition route) {
        if (Objects.isNull(route))
            return;

        this.routes.add(route);
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n\t").append("路由:").append("\t").append(route.getId());
        StringJoiner predicateJ = new StringJoiner(";\t");
        List<PredicateDefinition> predicates = route.getPredicates();
        if (CollectionUtils.isNotEmpty(predicates)) {
            for (PredicateDefinition predicate : predicates) {
                String name = predicate.getName();
                Map<String, String> args = predicate.getArgs();
                if (MapUtils.isEmpty(args)) {
                    predicateJ.add(name);
                } else {
                    predicateJ.add(name + ":" + JsonUtil.instance.toStr(args));
                }
            }
            sb.append("\r\n\t").append("规则:").append("\t").append(predicateJ);
        }

        sb.append("\r\n\t").append("转发:").append("\t").append(route.getUri());
        List<FilterDefinition> filters = route.getFilters();
        StringJoiner filterJ = new StringJoiner(";\t");
        if (CollectionUtils.isNotEmpty(filters)) {
            for (FilterDefinition filter : filters) {
                String name = filter.getName();
                Map<String, String> args = filter.getArgs();
                if (MapUtils.isEmpty(args)) {
                    filterJ.add(name);
                } else {
                    filterJ.add(name + ":" + JsonUtil.instance.toStr(args));
                }
            }
            sb.append("\r\n\t").append("拦截:").append("\t").append(filterJ);
        }

        routeLog.add(sb);
    }*/

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
        String path = "/api/" + prefix + "/" + routeNode.getPath() + "/**";
        boolean enableAuth = routeNode.enableAuth();
        String service = routeNode.getService();
        String remark = routeNode.getRemark();
        URI uri = URI.create("lb://" + service);

        StringBuilder sb = new StringBuilder();
        sb.append("\r\n\t").append("路由:").append("\t").append(name);
        sb.append("\r\n\t").append("规则:").append("\t").append(path);
        sb.append("\r\n\t").append("转发:").append("\t").append(uri);
        sb.append("\r\n\t").append("备注:").append("\t").append(remark);

        definition.setId(name);
        definition.setUri(uri);

        // 设置Predicate
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        predicate.addArg("pattern", path);
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
        filterJ.add("RewritePath=" + path + "-> /" + routeNode.getPath() + "/**");

        // 添加全局过滤器（通过配置方式）
        if (enableAuth) {
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