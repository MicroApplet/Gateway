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

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.asialjim.microapplet.common.utils.JacksonUtil;
import com.asialjim.microapplet.gateway.config.NacosRouteDefinitionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 路由配置变更监听器
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/9/25, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Configuration
public class RouteChangedListener implements Listener, DisposableBean {
    private static final JacksonUtil yamlUtil = JacksonUtil.instance(new ObjectMapper(new YAMLFactory()));
    private static final String dataId = "route.yaml";

    private final NacosRouteDefinitionRepository nacosRouteDefinitionRepository;
    private final NacosConfigManager nacosConfigManager;
    private final Executor executor;
    private boolean isCustomExecutor = false; // 标记是否使用了自定义的线程池

    @Value("${spring.cloud.nacos.discovery.group}")
    private String group;

    public RouteChangedListener(List<Executor> executors,
                                NacosConfigManager nacosConfigManager,
                                NacosRouteDefinitionRepository nacosRouteDefinitionRepository) {
        this.executor = Optional.ofNullable(executors)
                .stream()
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .findAny()
                .orElseGet(() -> {
                    // 返回自定义线程池，避免使用默认线程池可能导致的问题
                    isCustomExecutor = true;
                    return Executors.newSingleThreadExecutor(r -> {
                        Thread thread = new Thread(r, "route-refresh-executor");
                        thread.setDaemon(true);
                        return thread;
                    });
                });
        this.nacosConfigManager = nacosConfigManager;
        this.nacosRouteDefinitionRepository = nacosRouteDefinitionRepository;
    }

    @PostConstruct
    public void init() {
        try {
            // 检查必要依赖
            if (nacosConfigManager == null) {
                log.error("NacosConfigManager未注入，无法初始化配置监听");
                return;
            }
            
            if (nacosRouteDefinitionRepository == null) {
                log.error("NacosRouteDefinitionRepository未注入，无法刷新路由");
                return;
            }

            ConfigService configService = nacosConfigManager.getConfigService();
            if (configService == null) {
                log.error("无法获取ConfigService，初始化失败");
                return;
            }
            
            log.info("开始初始化Nacos配置监听，dataId={}, group={}", dataId, group);
            
            // 1. 首次加载配置
            String configContent = configService.getConfig(dataId, group, 5000);
            if (configContent != null && !configContent.isEmpty()) {
                log.info("首次加载配置成功，开始初始化路由...");
                receiveConfigInfo(configContent);
            } else {
                log.warn("首次加载配置失败或配置为空");
            }
            
            // 2. 添加配置监听
            configService.addListener(dataId, group, this);
            log.info("Nacos配置监听初始化完成，已注册监听器");
        } catch (Exception e) {
            log.error("初始化Nacos配置监听失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public Executor getExecutor() {
        return this.executor;
    }

    @Override
    public void receiveConfigInfo(String configInfo) {
        try {
            log.info("收到路由配置更新");
            
            if (configInfo == null || configInfo.isEmpty()) {
                log.warn("配置内容为空，跳过处理");
                return;
            }
            
            GatewayRouteWrapper bean = yamlUtil.toBean(configInfo, GatewayRouteWrapper.class);
            if (bean != null && bean.getGateway() != null && bean.getGateway().getRoutes() != null && !bean.getGateway().getRoutes().isEmpty()) {
                nacosRouteDefinitionRepository.refreshRoutes(bean.getGateway());
                log.info("路由配置更新成功，共加载{}条路由", bean.getGateway().getRoutes().size());
            } else {
                log.warn("收到的路由配置为空或格式无效");
            }
        } catch (Exception e) {
            log.error("处理路由配置更新失败: {}", e.getMessage(), e);
            // 添加重试逻辑
            try {
                Thread.sleep(5000);
                ConfigService configService = nacosConfigManager.getConfigService();
                if (configService != null) {
                    String configContent = configService.getConfig(dataId, group, 5000);
                    if (configContent != null && !configContent.isEmpty()) {
                        log.info("尝试重新加载配置...");
                        receiveConfigInfo(configContent);
                    }
                }
            } catch (Exception retryEx) {
                log.error("重试加载配置失败: {}", retryEx.getMessage(), retryEx);
            }
        }
    }

    @Data
    private static class GatewayRouteWrapper {
        private RouteConfigProperty gateway;
    }
    
    @Override
    @PreDestroy
    public void destroy() {
        try {
            // 1. 移除Nacos配置监听器
            if (nacosConfigManager != null) {
                ConfigService configService = nacosConfigManager.getConfigService();
                if (configService != null) {
                    configService.removeListener(dataId, group, this);
                    log.info("已移除Nacos配置监听器，dataId={}, group={}", dataId, group);
                }
            }
            
            // 2. 关闭自定义创建的线程池
            if (isCustomExecutor && executor instanceof java.util.concurrent.ExecutorService) {
                try {
                    ((java.util.concurrent.ExecutorService) executor).shutdown();
                    log.info("已关闭自定义路由刷新线程池");
                } catch (Exception e) {
                    log.error("关闭线程池失败: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("销毁RouteChangedListener时发生异常: {}", e.getMessage(), e);
        }
    }
}