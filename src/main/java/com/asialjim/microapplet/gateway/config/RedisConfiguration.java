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

import com.asialjim.microapplet.common.utils.JacksonUtil;
import com.asialjim.microapplet.common.utils.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JacksonUtil.init(new ObjectMapper());
    }

    @Bean
    @Primary
    public GenericJackson2JsonRedisSerializer jsonSerializer() {
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer();
        json.configure(JsonUtil::init);
        return json;
    }

    @Bean
    @Primary
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory,
            GenericJackson2JsonRedisSerializer jsonSerializer) {

        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext()
                .string(StringRedisSerializer.UTF_8)
                .key(StringRedisSerializer.UTF_8)
                .value(jsonSerializer)
                .hashKey(StringRedisSerializer.UTF_8)
                .hashValue(jsonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, serializationContext);
    }
}
