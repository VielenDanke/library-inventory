package kz.danke.library.events.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface LibraryEventService {

    void processLibraryEvent(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException;

    void handleRecovery(ConsumerRecord<Integer, String> consumerRecord);
}
