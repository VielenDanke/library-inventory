package kz.danke.library.events.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.danke.library.events.entity.Book;
import kz.danke.library.events.entity.LibraryEvent;
import kz.danke.library.events.repository.BookRepository;
import kz.danke.library.events.repository.LibraryEventRepository;
import kz.danke.library.events.service.LibraryEventService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class LibraryEventServiceImpl implements LibraryEventService {

    private final ObjectMapper objectMapper;
    private final LibraryEventRepository libraryEventRepository;
    private final BookRepository bookRepository;

    @Autowired
    public LibraryEventServiceImpl(ObjectMapper objectMapper,
                                   LibraryEventRepository libraryEventRepository,
                                   BookRepository bookRepository) {
        this.objectMapper = objectMapper;
        this.libraryEventRepository = libraryEventRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void processLibraryEvent(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        String value = consumerRecord.value();

        LibraryEvent libraryEvent = objectMapper.readValue(value, LibraryEvent.class);

        log.info("Library event: {}", libraryEvent);

        switch (libraryEvent.getLibraryEventType()) {
            case NEW:
                saveLibraryEventWithBook(libraryEvent, libraryEvent.getBook());
                break;
            case UPDATE:
                if (libraryEvent.getLibraryEventId() == null) {
                    throw new IllegalArgumentException("Library event ID is missing");
                }
                LibraryEvent existsLibraryEvent = libraryEventRepository
                        .findByLibraryEventId(libraryEvent.getLibraryEventId())
                        .orElseThrow(() -> new IllegalArgumentException("Library event doesn't exist"));

                saveLibraryEventWithBook(existsLibraryEvent, libraryEvent.getBook());
                break;
            default:
                throw new RuntimeException();
        }
    }

    private void saveLibraryEventWithBook(LibraryEvent libraryEvent, Book book) {
        libraryEvent.addBook(book);
        libraryEventRepository.save(libraryEvent);
    }
}
