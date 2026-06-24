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

package com.asialjim.microapplet.gateway.filter;
import com.asialjim.microapplet.web.client.MamsHttpHeaders;

import com.asialjim.microapplet.commons.standard.context.Res;
import com.asialjim.microapplet.commons.standard.context.Result;
import com.asialjim.microapplet.commons.standard.exception.BusinessException;
import org.apache.commons.collections4.CollectionUtils;
import com.asialjim.microapplet.commons.standard.utils.JsonUtil;
import com.asialjim.microapplet.gateway.context.GatewayResCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 全局异常拦截处理器
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/27, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Slf4j
@Component
public class GlobalExceptionFilter implements GatewayFilter {
    public static final String name = "GlobalExceptionFilter";

    public static class Config {
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(t -> {
                    try {
                        String trace = exchange.getAttribute(MamsHttpHeaders.TRACE_ID);
                        if (StringUtils.isNotBlank(trace))
                            MDC.put(MamsHttpHeaders.TRACE_ID, trace);
                        ServerHttpResponse response = exchange.getResponse();
                        Result<Object> result;
                        if (t instanceof BusinessException apiException) {
                            log.error("网关业务异常：{}", apiException.detailMessage());
                            result = apiException.create();
                        } else if (t instanceof NotFoundException notFoundException) {
                            log.info("网关未找到服务：{}", notFoundException.getMessage());
                            result = GatewayResCode.ServiceNotFound.resultErrs(Collections.singletonList(notFoundException.getMessage()));
                        } else {
                            if (log.isDebugEnabled())
                                log.debug("网关探测到不明错误：{}", t.getMessage(), t);
                            else
                                log.error("网关探测到不明错误：{}", t.getMessage());
                            result = Res.SysErr.resultErrs(List.of("网关错误"));
                        }

                        List<MediaType> accept = exchange.getRequest().getHeaders().getAccept();
                        if (CollectionUtils.size(accept) == 1) {
                            MediaType first = accept.getFirst();
                            String acceptStr = first.toString();
                            if (StringUtils.isNotBlank(acceptStr) && acceptStr.startsWith(MediaType.TEXT_EVENT_STREAM_VALUE)){
                                response.setStatusCode(HttpStatus.OK);
                                response.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
                                DataBufferFactory factory = response.bufferFactory();
                                List<Event> list = new ArrayList<>();
                                list.add(Event.builder()
                                        .status(result.getStatus())
                                        .success(!result.isThr())
                                        .code(result.getCode())
                                        .msg(result.getMsg())
                                        .event("error")
                                        .messageId(UUID.randomUUID().toString())
                                        .taskId(UUID.randomUUID().toString())
                                        .errs(result.getErrs())
                                        .busitype("0")
                                        .answer("API调用错误，代码：" + result.getCode() + "; 信息：" + result.getMsg())
                                        .build());

                                List<DataBuffer> res = list.stream()
                                        .map(JsonUtil.instance::toStr)
                                        .map(item -> "data:" + item + "\n\n")
                                        .map(item -> "event:error\n" + item)
                                        .map(item -> item.getBytes(StandardCharsets.UTF_8))
                                        .map(factory::wrap)
                                        .toList();

                                return response.writeWith(Flux.fromIterable(res));
                            }
                        }

                        String json = JsonUtil.instance.toStr(result);
                        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
                        response.setStatusCode(HttpStatusCode.valueOf(result.getStatus()));
                        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        return response.writeWith(Mono.just(buffer));
                    } finally {
                        MDC.clear();
                    }

                })
                .doFinally(t -> MDC.clear());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class Event implements Serializable {

        private int status;
        private boolean success;
        private String code;
        private String msg;
        private String event;
        private String answer;
        private String messageId;
        private String taskId;
        private List<String> errs;
        private String busitype;
    }
}