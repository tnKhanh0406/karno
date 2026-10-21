package com.karnoorder.config;

import com.karnoorder.constants.EventTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic inventoryReservationRequestedTopic() {

        return TopicBuilder
                .name(EventTopics.INVENTORY_RESERVATION_REQUESTED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
