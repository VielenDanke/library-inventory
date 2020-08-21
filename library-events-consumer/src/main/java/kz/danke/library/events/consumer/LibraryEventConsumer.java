package kz.danke.library.events.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.danke.library.events.service.LibraryEventService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static kz.danke.library.events.util.ConsumerUtil.LIBRARY_EVENTS_TOPIC_NAME;

@Component
@Slf4j
public class LibraryEventConsumer {

    private final LibraryEventService libraryEventService;

    @Autowired
    public LibraryEventConsumer(LibraryEventService libraryEventService) {
        this.libraryEventService = libraryEventService;
    }

    @KafkaListener(
            topics = {LIBRARY_EVENTS_TOPIC_NAME}
    )
    public void onMessage(ConsumerRecord<Integer, String> libraryEventConsumerRecord) throws JsonProcessingException {
        log.info("Consumer record: {}", libraryEventConsumerRecord);

        libraryEventService.processLibraryEvent(libraryEventConsumerRecord);
    }
}
