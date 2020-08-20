package kz.danke.library.events.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.danke.library.events.domain.Book;
import kz.danke.library.events.domain.LibraryEvent;
import kz.danke.library.events.domain.LibraryEventType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(
        properties = {
                "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"
        }
)
public class LibraryEventsControllerIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<Integer, String> consumer;

    @BeforeEach
    public void setup() {
        Map<String, Object> configs = new HashMap<>(
                KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker)
        );

        consumer = new DefaultKafkaConsumerFactory<>(
                configs,
                IntegerDeserializer::new,
                StringDeserializer::new
        ).createConsumer();

        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    public void tearDown() {
        consumer.close();
    }

    @Test
    public void postLibraryEvent() throws JsonProcessingException {
        Book book = Book.builder()
                .bookId(123)
                .bookName("The New Book")
                .bookAuthor("Danke")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(book)
                .build();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LibraryEvent> requestBodyLibraryEvent = new HttpEntity<>(libraryEvent, headers);

        ResponseEntity<LibraryEvent> responseEntityLibraryEvent = testRestTemplate
                .exchange(
                        "/v1/library-events",
                        HttpMethod.POST,
                        requestBodyLibraryEvent,
                        LibraryEvent.class
                );
        ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, "library-events");

        String value = singleRecord.value();

        LibraryEvent fromKafkaLibraryEvent = objectMapper.readValue(value, LibraryEvent.class);
        LibraryEvent entityLibraryEventBody = responseEntityLibraryEvent.getBody();

        Assertions.assertEquals(HttpStatus.CREATED, responseEntityLibraryEvent.getStatusCode());
        Assertions.assertEquals(entityLibraryEventBody, fromKafkaLibraryEvent);
    }
}
