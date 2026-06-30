package dev.maliik.relaycore.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import dev.maliik.relaycore.common.event.Topics;

/**
 * Declares the event-loop topics so they exist at startup (created by {@code KafkaAdmin}). Declaring
 * them up front means consumers get a partition assignment immediately rather than waiting on broker
 * metadata discovery for a not-yet-existent topic. Single partition / single replica suits the local
 * Redpanda broker; the managed prod cluster (Upstash) is provisioned out of band.
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic matchCompletedTopic() {
        return TopicBuilder.name(Topics.MATCH_COMPLETED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic rewardGrantedTopic() {
        return TopicBuilder.name(Topics.REWARD_GRANTED).partitions(1).replicas(1).build();
    }
}
