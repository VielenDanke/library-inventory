package kz.danke.library.events.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.danke.library.events.domain.Book;
import kz.danke.library.events.domain.LibraryEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.ExecutionException;

@ExtendWith(MockitoExtension.class)
public class LibraryEventProducerUnitTest {

    @InjectMocks
    private LibraryEventProducer libraryEventProducer;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private KafkaTemplate<Integer, String> kafkaTemplate;

    @Test
    public void testSendLibraryEvent() throws JsonProcessingException, ExecutionException, InterruptedException {
        Book book = Book.builder()
                .bookId(123)
                .bookName("The New Book")
                .bookAuthor("Danke")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(book)
                .build();

        String libraryEventJson = objectMapper.writeValueAsString(libraryEvent);

        TopicPartition topicPartition = new TopicPartition("library-events", 1);

        ProducerRecord<Integer, String> producerRecord = new ProducerRecord<>(
                "library-events",
                libraryEvent.getLibraryEventId(),
                libraryEventJson
        );
        RecordMetadata recordMetadata = new RecordMetadata(
                topicPartition,
                1L,
                1L,
                System.currentTimeMillis(),
                342L,
                1,
                2
        );

        SettableListenableFuture<SendResult<Integer, String>> future = new SettableListenableFuture<>();

        SendResult<Integer, String> sendResult = new SendResult<>(
                producerRecord,
                recordMetadata
        );
        future.set(sendResult);

        Mockito.when(kafkaTemplate.send(Mockito.isA(String.class), Mockito.isNull(), Mockito.isA(String.class)))
                .thenReturn(future);

        ListenableFuture<SendResult<Integer, String>> listenableFuture = libraryEventProducer.sendLibraryEvent(libraryEvent);

        SendResult<Integer, String> result = listenableFuture.get();

        Assertions.assertEquals(result.getRecordMetadata().partition(), 1);
    }

    @Test
    public void testSendLibraryEvent_Failure() throws JsonProcessingException, ExecutionException, InterruptedException {
        Book book = Book.builder()
                .bookId(123)
                .bookName("The New Book")
                .bookAuthor("Danke")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(book)
                .build();

        SettableListenableFuture<SendResult<Integer, String>> future = new SettableListenableFuture<>();

        future.setException(new RuntimeException("Exception calling Kafka"));

        Mockito.when(kafkaTemplate.send(Mockito.isA(String.class), Mockito.isA(Integer.class), Mockito.isA(String.class)))
                .thenReturn(future);

        Assertions.assertThrows(
                RuntimeException.class,
                () -> libraryEventProducer.sendLibraryEvent(libraryEvent).get()
        );
    }
}
