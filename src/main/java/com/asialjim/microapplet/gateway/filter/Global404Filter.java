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

package com.asialjim.microapplet.gateway.filter;

import com.asialjim.microapplet.common.context.Result;
import com.asialjim.microapplet.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 动态路由过滤，未配置的路由返回404
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025年9月24日, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
public class Global404Filter implements GatewayFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 关键：检查GATEWAY_ROUTE_ATTR属性，如果请求已被路由，该属性会被设置
        // 如果此属性为null，说明没有任何路由配置匹配当前请求
        if (exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR) == null)
            return response404(exchange);

        // 如果请求已被路由，继续执行过滤器链
        return chain.filter(exchange);
    }

    public static Mono<Void> response404(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.getHeaders().add("Content-Type", "application/json");

        // 构建404响应体
        Result<String> res = new Result<String>().setStatus(404).setThr(true).setCode("PAGE:NOT:FOUND").setMsg("资源不存在").setData("Page Not Found");
        byte[] bytes = JsonUtil.instance.toStr(res).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        //  order 设置为一个很高的值，确保在路由确定之后执行
        return Ordered.LOWEST_PRECEDENCE;
    }
}
