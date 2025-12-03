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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class LogHttpResDecorator extends ServerHttpResponseDecorator implements DisposableBean {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final AtomicBoolean bodyCollected = new AtomicBoolean(false);
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final AtomicReference<StringBuffer> bodyCache;
    private final StopWatch stopWatch;
    private final String startTimeStr;
    private final String trace;

    // 设置最大缓存大小，防止过大的响应体占用过多内存
    private static final int MAX_BODY_CACHE_SIZE = 1024 * 1024; // 1MB

    public LogHttpResDecorator(String trace,
                               StopWatch stopWatch,
                               ServerWebExchange exchange) {
        super(exchange.getResponse());
        this.startTimeStr = LocalDateTime.now().format(FORMATTER);
        this.stopWatch = stopWatch;
        this.bodyCache = new AtomicReference<>();
        this.bodyCache.set(new StringBuffer());
        this.trace = trace;
        this.stopWatch.start();
    }

    /**
     * 从响应头中获取字符集编码
     *
     * @return 字符集编码，默认使用 UTF-8
     */
    private Charset getCharsetFromHeaders() {
        String contentType = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType != null) {
            try {
                // 从 Content-Type 中解析字符集，例如：text/html;charset=UTF-8
                if (contentType.contains("charset=")) {
                    String charsetStr = contentType.split("charset=")[1].split("[; ]")[0];
                    return Charset.forName(charsetStr.trim());
                }
            } catch (Exception e) {
                // 如果解析失败，使用默认编码
            }
        }
        return StandardCharsets.UTF_8;
    }


    @Override
    @SuppressWarnings("NullableProblems")
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        try {
            addTimeHeaders();

            if (body instanceof Mono<? extends DataBuffer> mono) {
                Mono<DataBuffer> map = mono.map(this::cacheBody);
                return super.writeWith(map);
            } else if (body instanceof Flux<? extends DataBuffer> flux) {
                Flux<DataBuffer> map = flux.map(this::cacheBody);
                return super.writeWith(map);
            } else
                return super.writeWith(body);

        } finally {
            complete.set(true);
        }
    }

    private DataBuffer cacheBody(DataBuffer dataBuffer) {
        if (!bodyCollected.compareAndSet(false, true))
            return dataBuffer; // 已经收集过，避免重复收集

        DataBuffer retain = DataBufferUtils.retain(dataBuffer);
        try {
            StringBuffer stringBuffer = bodyCache.get();
            // 检查当前缓存大小，超过限制则不再添加
            if (stringBuffer.length() < MAX_BODY_CACHE_SIZE) {
                Charset charset = getCharsetFromHeaders();
                String content = retain.toString(charset);

                // 如果添加当前内容会超过最大大小，则截断内容
                if (stringBuffer.length() + content.length() > MAX_BODY_CACHE_SIZE) {
                    int remainingSize = MAX_BODY_CACHE_SIZE - stringBuffer.length();
                    content = content.substring(0, remainingSize) + "... [Content truncated due to size limit]";
                }
                stringBuffer.append(content);
            }
        } finally {
            // 确保在使用后释放保留的DataBuffer
            DataBufferUtils.release(retain);
        }
        // 返回原始的dataBuffer而不是retain
        return dataBuffer;
    }

    private Mono<String> responseBody() {
        return Mono.fromCallable(() -> {
            complete.get();

            StringBuffer stringBuffer = bodyCache.get();
            String body = stringBuffer != null ? stringBuffer.toString() : StringUtils.EMPTY;
            if (StringUtils.isBlank(body)) {
                return StringUtils.EMPTY;
            }
            return "<<< 响应体: \r\n\t\t" + body;
        });
    }

    public Flux<String> responseLogFlux() {
        return Flux.concat(
                Mono.just(responseLine()),
                Mono.just(responseHeader()),
                responseBody()
        );
    }

    private String responseLine() {
        return "<<< 响应行: " + getStatusCode();
    }

    private String responseHeader() {
        HttpHeaders headers = getHeaders();
        StringJoiner headJoiner = new StringJoiner("\r\n\t\t");
        headJoiner.add(StringUtils.EMPTY);
        headers.forEach((k, v) -> headJoiner.add(k + " -> " + String.join(",", v)));
        return "<<< 响应头: " + headJoiner;
    }

    /**
     * 清理缓存资源，避免内存泄漏
     */
    private void clearCache() {
        StringBuffer buffer = bodyCache.getAndSet(null);
        if (buffer != null) {
            buffer.setLength(0); // 清空内容，帮助垃圾回收
        }
    }

    /**
     * 应用销毁时调用，清理缓存资源
     */
    @Override
    public void destroy() {
        clearCache();
    }


    private void addTimeHeaders() {
        this.stopWatch.stop();
        long time = this.stopWatch.getTime(TimeUnit.MILLISECONDS);

        HttpHeaders headers = getHeaders();
        headers.set("X-Trace-Id", this.trace);
        headers.set("X-Request-Time", this.startTimeStr);
        headers.set("X-Request-Cost", time + " ms");
        headers.set("X-Response-Time", LocalDateTime.now().format(FORMATTER));
    }
}