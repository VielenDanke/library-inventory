package kz.danke.library.events.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static kz.danke.library.events.util.ConsumerUtil.LIBRARY_EVENTS_TOPIC_NAME;

//@Component
@Slf4j
public class LibraryEventConsumerManualOffset implements AcknowledgingMessageListener<Integer, String> {

    @Override
//    @KafkaListener(topics = {LIBRARY_EVENTS_TOPIC_NAME})
    public void onMessage(ConsumerRecord<Integer, String> data, Acknowledgment acknowledgment) {
        log.info("Consumer record: {}", data);
        acknowledgment.acknowledge();
    }
}
