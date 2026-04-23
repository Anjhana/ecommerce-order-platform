package com.anjhana.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Topics ───────────────────────────────────────────────────────────────

    @Bean public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.created").partitions(3).replicas(1).build();
    }
    @Bean public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name("order.confirmed").partitions(3).replicas(1).build();
    }
    @Bean public NewTopic orderCancelledTopic() {
        return TopicBuilder.name("order.cancelled").partitions(3).replicas(1).build();
    }

    // ── Producer ─────────────────────────────────────────────────────────────
    @Primary
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");                   // strongest durability
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);      // exactly-once producer
        //return new DefaultKafkaProducerFactory<>(props);
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>();
        jsonSerializer.setAddTypeInfo(false); 

        // Use the constructor to FORCE the serializers
        return new DefaultKafkaProducerFactory<>(
            props, 
            new StringSerializer(), 
            jsonSerializer
        );
        
    }
    
    @Primary
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
