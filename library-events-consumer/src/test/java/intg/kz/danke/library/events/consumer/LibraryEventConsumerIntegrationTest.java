package kz.danke.library.events.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.danke.library.events.entity.Book;
import kz.danke.library.events.entity.LibraryEvent;
import kz.danke.library.events.entity.LibraryEventType;
import kz.danke.library.events.repository.BookRepository;
import kz.danke.library.events.repository.LibraryEventRepository;
import kz.danke.library.events.service.LibraryEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
public class LibraryEventConsumerIntegrationTest {

    private static final int numberOfInvocations = 1;
    private static final int expectedSize = 1;
    private static final int timeout = 3;
    private static final int countOfThread = 1;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LibraryEventRepository libraryEventRepository;

    @Autowired
    private BookRepository bookRepository;

    @SpyBean
    private LibraryEventService libraryEventServiceSpy;

    @SpyBean
    private LibraryEventConsumer libraryEventConsumerSpy;

    @BeforeEach
    public void setup() {
        for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry.getListenerContainers()) {
                ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @AfterEach
    public void tearDown() {
        bookRepository.deleteAll();
        libraryEventRepository.deleteAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void publishNewLibraryEvent() throws Exception {
        int bookId = new Random().nextInt(1000);

        Book book = Book.builder()
                .bookId(bookId)
                .bookName("The New Book")
                .bookAuthor("Danke")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .libraryEventType(LibraryEventType.NEW)
                .book(book)
                .build();

        String libraryEventJson = objectMapper.writeValueAsString(libraryEvent);

        kafkaTemplate.sendDefault(libraryEventJson).get();

        CountDownLatch latch = new CountDownLatch(countOfThread);

        latch.await(timeout, TimeUnit.SECONDS);

        Mockito
                .verify(libraryEventConsumerSpy, Mockito.times(numberOfInvocations))
                .onMessage(Mockito.isA(ConsumerRecord.class));

        Mockito
                .verify(libraryEventServiceSpy, Mockito.times(numberOfInvocations))
                .processLibraryEvent(Mockito.isA(ConsumerRecord.class));

        List<LibraryEvent> eventList = libraryEventRepository
                .findAll();

        eventList
                .stream()
                .findFirst()
                .ifPresent(event -> {
                    Assertions.assertEquals(bookId, event.getBook().getBookId());
                });

        int size = eventList.size();

        Assertions.assertEquals(expectedSize, size);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void publishUpdateLibraryEvent() throws Exception {
        int bookId = new Random().nextInt(1000);

        Book book = Book.builder()
                .bookId(bookId)
                .bookName("The New Book")
                .bookAuthor("Danke")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .libraryEventType(LibraryEventType.UPDATE)
                .build();

        libraryEvent.addBook(book);

        LibraryEvent savedLibraryEvent = libraryEventRepository.save(libraryEvent);

        String savedLibraryEventJson = objectMapper.writeValueAsString(savedLibraryEvent);

        kafkaTemplate.sendDefault(savedLibraryEventJson).get();

        CountDownLatch latch = new CountDownLatch(countOfThread);

        latch.await(timeout, TimeUnit.SECONDS);

        Mockito
                .verify(libraryEventConsumerSpy, Mockito.atLeast(numberOfInvocations))
                .onMessage(Mockito.isA(ConsumerRecord.class));

        Mockito
                .verify(libraryEventServiceSpy, Mockito.atLeast(numberOfInvocations))
                .processLibraryEvent(Mockito.isA(ConsumerRecord.class));

        List<LibraryEvent> eventList = libraryEventRepository
                .findAll();

        eventList
                .stream()
                .findFirst()
                .ifPresent(event -> {
                    Assertions.assertEquals(bookId, event.getBook().getBookId());
                });

        int size = eventList.size();

        Assertions.assertEquals(expectedSize, size);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void publishUpdateLibraryEvent_Failure() throws Exception {
        int bookId = new Random().nextInt(1000);

        Book book = Book.builder()
                .bookId(bookId)
                .bookName("The New Book")
                .bookAuthor("Danke")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .libraryEventType(LibraryEventType.NEW)
                .book(book)
                .build();

        LibraryEvent savedLibraryEvent = libraryEventRepository.save(libraryEvent);

        savedLibraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        savedLibraryEvent.setLibraryEventId(Integer.MAX_VALUE);

        String savedLibraryEventJson = objectMapper.writeValueAsString(savedLibraryEvent);

        kafkaTemplate.sendDefault(savedLibraryEventJson);

        CountDownLatch latch = new CountDownLatch(countOfThread);

        latch.await(timeout, TimeUnit.SECONDS);

        Mockito
                .verify(libraryEventConsumerSpy, Mockito.atLeast(numberOfInvocations))
                .onMessage(Mockito.isA(ConsumerRecord.class));

        Mockito
                .verify(libraryEventServiceSpy, Mockito.atLeast(numberOfInvocations))
                .processLibraryEvent(Mockito.isA(ConsumerRecord.class));

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> libraryEventRepository.findById(Integer.MAX_VALUE)
                .orElseThrow(IllegalArgumentException::new)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void publishUpdateLibraryEvent_FailureWithNullId() throws Exception {
        int bookId = new Random().nextInt(1000);

        Book book = Book.builder()
                .bookId(bookId)
                .bookName("The New Book")
                .bookAuthor("Danke")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .libraryEventType(LibraryEventType.NEW)
                .book(book)
                .build();

        LibraryEvent savedLibraryEvent = libraryEventRepository.save(libraryEvent);

        savedLibraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        savedLibraryEvent.setLibraryEventId(null);

        String savedLibraryEventJson = objectMapper.writeValueAsString(savedLibraryEvent);

        kafkaTemplate.sendDefault(savedLibraryEventJson);

        CountDownLatch latch = new CountDownLatch(countOfThread);

        latch.await(timeout, TimeUnit.SECONDS);

        Mockito
                .verify(libraryEventConsumerSpy, Mockito.atLeast(numberOfInvocations))
                .onMessage(Mockito.isA(ConsumerRecord.class));

        Mockito
                .verify(libraryEventServiceSpy, Mockito.atLeast(numberOfInvocations))
                .processLibraryEvent(Mockito.isA(ConsumerRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void publishUpdateLibraryEvent_FailureWithZeroId() throws Exception {
        final int zeroLibraryEventId = 0;

        int bookId = new Random().nextInt(1000);

        Book book = Book.builder()
                .bookId(bookId)
                .bookName("The New Book")
                .bookAuthor("Danke")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(zeroLibraryEventId)
                .libraryEventType(LibraryEventType.NEW)
                .book(book)
                .build();

        String libraryEventJson = objectMapper.writeValueAsString(libraryEvent);

        kafkaTemplate.sendDefault(libraryEventJson);

        CountDownLatch latch = new CountDownLatch(countOfThread);

        latch.await(timeout, TimeUnit.SECONDS);

//        Mockito
//                .verify(libraryEventConsumerSpy, Mockito.times(3))
//                .onMessage(Mockito.isA(ConsumerRecord.class));
        Mockito
                .verify(libraryEventServiceSpy, Mockito.times(4))
                .processLibraryEvent(Mockito.isA(ConsumerRecord.class));

        Mockito
                .verify(libraryEventServiceSpy, Mockito.times(1))
                .handleRecovery(Mockito.isA(ConsumerRecord.class));
    }
}
