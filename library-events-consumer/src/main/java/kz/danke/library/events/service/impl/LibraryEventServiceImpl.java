package kz.danke.library.events.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.danke.library.events.entity.Book;
import kz.danke.library.events.entity.LibraryEvent;
import kz.danke.library.events.repository.LibraryEventRepository;
import kz.danke.library.events.service.LibraryEventService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Service
@Slf4j
public class LibraryEventServiceImpl implements LibraryEventService {

    private final ObjectMapper objectMapper;
    private final LibraryEventRepository libraryEventRepository;
    private final KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    public LibraryEventServiceImpl(ObjectMapper objectMapper,
                                   LibraryEventRepository libraryEventRepository,
                                   KafkaTemplate<Integer, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.libraryEventRepository = libraryEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void processLibraryEvent(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        String value = consumerRecord.value();

        LibraryEvent libraryEvent = objectMapper.readValue(value, LibraryEvent.class);

        if (libraryEvent.getLibraryEventId() != null && libraryEvent.getLibraryEventId() == 0) {
            throw new RecoverableDataAccessException("Temporary network issue");
        }
        log.info("Library event: {}", libraryEvent);

        Book book = libraryEvent.getBook();

        switch (libraryEvent.getLibraryEventType()) {
            case NEW:
                saveLibraryEventWithBook(libraryEvent, book);
                break;
            case UPDATE:
                if (libraryEvent.getLibraryEventId() == null) {
                    throw new IllegalArgumentException("Library event ID is missing");
                }
                LibraryEvent existsLibraryEvent = libraryEventRepository
                        .findByLibraryEventId(libraryEvent.getLibraryEventId())
                        .orElseThrow(() -> new IllegalArgumentException("Library event doesn't exist"));

                saveLibraryEventWithBook(existsLibraryEvent, book);
                break;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void handleRecovery(ConsumerRecord<Integer, String> consumerRecord) {
        Integer key = consumerRecord.key();
        String value = consumerRecord.value();

        ListenableFuture<SendResult<Integer, String>> listenableFuture = kafkaTemplate.sendDefault(key, value);

        listenableFuture.addCallback(new ListenableFutureCallback<SendResult<Integer, String>>() {
            @Override
            public void onFailure(Throwable ex) {
                handleFailure(key, value, ex);
            }

            @Override
            public void onSuccess(SendResult<Integer, String> result) {
                handleSuccess(key, value, result);
            }
        });
    }

    private void saveLibraryEventWithBook(LibraryEvent libraryEvent, Book book) {
        libraryEvent.addBook(book);
        libraryEventRepository.save(libraryEvent);
    }

    private void handleFailure(Integer key, String value, Throwable ex) {
        log.error(String.format("Error is: %s", ex.getClass().getSimpleName()));
        try {
            throw ex;
        } catch (Throwable e) {
            log.error(String.format("Error on failure is: %s", e.getClass().getSimpleName()));
        }
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> result) {
        log.info(String.format("Message for the key %d, value %s, partition %d",
                key,
                value,
                result.getRecordMetadata().partition()));
    }
}
