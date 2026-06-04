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

package com.asialjim.microapplet.gateway.cloud;

import com.asialjim.microapplet.common.exception.RsEx;
import com.asialjim.microapplet.common.utils.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.asialjim.microapplet.common.cons.Headers.*;


/**
 * 用户认证负载均衡
 *
 * @author <a href="mailto:asialjim@hotmail.com">Asial Jim</a>
 * @version 1.0
 * @since 2025/9/24, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Configuration
public class AuthServiceLoadBalancerConfig {

    public static Function<ClientResponse, Mono<? extends Throwable>> rsExFunction() {
        return clientResponse -> {
            HttpHeaders httpHeaders = clientResponse.headers().asHttpHeaders();
            boolean throwable = Boolean.parseBoolean(firstValue(X_RES_THROWABLE,httpHeaders));
            String code = firstValue(X_RES_CODE,httpHeaders);
            String msg = firstValue(X_RES_MSG, httpHeaders);
            String errorJson = firstValue(X_RES_ERRS,httpHeaders);
            List<String> errs = StringUtils.isBlank(errorJson) ? Collections.emptyList(): JsonUtil.instance.toList(errorJson, String.class);
            int status = NumberUtils.toInt(firstValue(X_RES_STATUS,httpHeaders), clientResponse.statusCode().value());
            // 处理错误状态码，例如 401, 403 等
            return Mono.error(
                    new RsEx()
                            .setStatus(status)
                            .setThr(throwable)
                            .setCode(code)
                            .setMsg(msg)
                            .setErrs(errs)
            );
        };
    }

    private static String firstValue(String key , HttpHeaders httpHeaders){
        String value = httpHeaders.getFirst(key);
        if (StringUtils.isBlank(value))
            return StringUtils.EMPTY;
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
