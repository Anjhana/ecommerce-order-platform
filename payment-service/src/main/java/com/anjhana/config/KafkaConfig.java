package com.anjhana.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

@Configuration
public class KafkaConfig {

    @Bean
    public RecordMessageConverter converter() {
        // This is the "magic" that maps JSON to your Map<String, Object> parameter
        return new StringJsonMessageConverter();
    }
}

