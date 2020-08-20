package kz.danke.library.events.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static kz.danke.library.events.util.TopicConstants.LIBRARY_EVENTS_TOPIC_NAME;

@Configuration
public class TopicCreationConfig {

    @Bean
    public NewTopic libraryEventTopic() {
        return TopicBuilder
                .name(LIBRARY_EVENTS_TOPIC_NAME)
                .partitions(3)
                .replicas(3)
                .build();
    }
}
