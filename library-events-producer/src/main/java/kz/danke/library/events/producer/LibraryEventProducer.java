package kz.danke.library.events.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.danke.library.events.domain.LibraryEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static kz.danke.library.events.util.TopicConstants.LIBRARY_EVENTS_TOPIC_NAME;

@Component
@Slf4j
public class LibraryEventProducer {

    private final KafkaTemplate<Integer, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public LibraryEventProducer(KafkaTemplate<Integer, String> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public ListenableFuture<SendResult<Integer, String>> sendLibraryEvent(LibraryEvent libraryEvent) throws JsonProcessingException {
        RecordHeader header = new RecordHeader("event-source", "scanner".getBytes());

        List<Header> recordHeaders = List.of(header);

        ProducerRecord<Integer, String> producerRecord = createProducerRecord(
                libraryEvent, recordHeaders
        );
        ListenableFuture<SendResult<Integer, String>> resultListenableFuture = kafkaTemplate.send(
                producerRecord
        );
        resultListenableFuture.addCallback(
                new ListenableFutureCallback<>() {
                    @Override
                    public void onFailure(Throwable ex) {
                        handleFailure(libraryEvent, ex);
                    }

                    @Override
                    public void onSuccess(SendResult<Integer, String> result) {
                        try {
                            handleSuccess(libraryEvent, result);
                        } catch (JsonProcessingException e) {
                            log.error(e.getLocalizedMessage(), e);
                        }
                    }
                }
        );
        return resultListenableFuture;
    }

    public SendResult<Integer, String> sendLibraryEventSynchronous(LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException {
        Integer key = libraryEvent.getLibraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);

        ListenableFuture<SendResult<Integer, String>> result = kafkaTemplate.send(
                LIBRARY_EVENTS_TOPIC_NAME,
                key,
                value
        );
        return result.get();
    }

    public CompletableFuture<SendResult<Integer, String>> sendLibraryEventUsingProducerRecord(LibraryEvent libraryEvent) throws JsonProcessingException {
        RecordHeader header = new RecordHeader("event-source", "scanner".getBytes());

        List<Header> recordHeaders = List.of(header);

        ProducerRecord<Integer, String> record = createProducerRecord(
                libraryEvent, recordHeaders
        );
        return kafkaTemplate.send(record)
                .completable();
    }

    private ProducerRecord<Integer, String> createProducerRecord(LibraryEvent libraryEvent, List<Header> headers) throws JsonProcessingException {
        String libraryEventJson = objectMapper.writeValueAsString(libraryEvent);

        return new ProducerRecord<>(
                LIBRARY_EVENTS_TOPIC_NAME,
                null,
                libraryEvent.getLibraryEventId(),
                libraryEventJson,
                headers
        );
    }


    private void handleFailure(LibraryEvent libraryEvent, Throwable ex) {
        log.error(String.format("Error is: %s", ex.getClass().getSimpleName()));
        try {
            throw ex;
        } catch (Throwable e) {
            log.error(String.format("Error on failure is: %s", e.getClass().getSimpleName()));
        }
    }

    private void handleSuccess(LibraryEvent libraryEvent, SendResult<Integer, String> result) throws JsonProcessingException {
        String libraryEventJson = objectMapper.writeValueAsString(libraryEvent);

        log.info(String.format("Message for the key %d, value %s, partition %d",
                libraryEvent.getLibraryEventId(),
                libraryEventJson,
                result.getRecordMetadata().partition()));
    }
}
