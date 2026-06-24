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

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicroBankGatewayFilterConfig {

    @Bean
    public AbstractGatewayFilterFactory<CustomerNotFoundFilter.Config> customerNotFoundFilterFactory(CustomerNotFoundFilter filter){
        return new AbstractGatewayFilterFactory<>(CustomerNotFoundFilter.Config.class) {
            @Override
            public GatewayFilter apply(CustomerNotFoundFilter.Config config) {
                return filter;
            }

            @Override
            public String name() {
                return CustomerNotFoundFilter.name;
            }
        };
    }

    @Bean
    public AbstractGatewayFilterFactory<AnnoFilter.Config> annoFilterFactory(AnnoFilter annoFilter) {
        return new AbstractGatewayFilterFactory<>(AnnoFilter.Config.class) {
            @Override
            public GatewayFilter apply(AnnoFilter.Config config) {
                return annoFilter;
            }

            @Override
            public String name() {
                return AnnoFilter.NAME;
            }
        };
    }

    @Bean
    public AbstractGatewayFilterFactory<AuthFilter.Config> authFilterFactory(AuthFilter authFilter) {
        return new AbstractGatewayFilterFactory<>(AuthFilter.Config.class) {
            @Override
            public GatewayFilter apply(AuthFilter.Config config) {
                return authFilter;
            }

            @Override
            public String name() {
                return AuthFilter.NAME;
            }
        };
    }

    @Bean
    public AbstractGatewayFilterFactory<DecryptFilter.Config> decryptFilterFactory(DecryptFilter decryptFilter) {
        return new AbstractGatewayFilterFactory<>(DecryptFilter.Config.class) {
            @Override
            public GatewayFilter apply(DecryptFilter.Config config) {
                return decryptFilter;
            }

            @Override
            public String name() {
                return DecryptFilter.NAME;
            }
        };
    }

    @Bean
    public AbstractGatewayFilterFactory<GlobalExceptionFilter.Config> gatewayFilterFactory(GlobalExceptionFilter globalExceptionFilter) {
        return new AbstractGatewayFilterFactory<>(GlobalExceptionFilter.Config.class) {
            @Override
            public GatewayFilter apply(GlobalExceptionFilter.Config config) {
                return globalExceptionFilter;
            }

            @Override
            public String name() {
                return GlobalExceptionFilter.name;
            }
        };
    }

    @Bean
    public AbstractGatewayFilterFactory<RouteFilter.Config> routeFilterFactory(RouteFilter routeFilter) {
        return new AbstractGatewayFilterFactory<>(RouteFilter.Config.class) {
            @Override
            public GatewayFilter apply(RouteFilter.Config config) {
                return routeFilter;
            }

            @Override
            public String name() {
                return RouteFilter.name;
            }
        };
    }

    @Bean
    public AbstractGatewayFilterFactory<TraceFilter.Config> traceFilterFactory(TraceFilter traceFilter) {
        return new AbstractGatewayFilterFactory<>(TraceFilter.Config.class) {
            @Override
            public GatewayFilter apply(TraceFilter.Config config) {
                return traceFilter;
            }

            @Override
            public String name() {
                return TraceFilter.name;
            }
        };
    }
}
