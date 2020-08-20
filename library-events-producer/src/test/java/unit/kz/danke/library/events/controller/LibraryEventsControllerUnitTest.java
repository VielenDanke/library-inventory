package kz.danke.library.events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.danke.library.events.domain.Book;
import kz.danke.library.events.domain.LibraryEvent;
import kz.danke.library.events.producer.LibraryEventProducer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.concurrent.SettableListenableFuture;

@WebMvcTest(LibraryEventsController.class)
@AutoConfigureMockMvc
public class LibraryEventsControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LibraryEventProducer libraryEventProducer;

    @Test
    public void postLibraryEvent() throws Exception {
        Book book = Book.builder()
                .bookId(123)
                .bookName("The New Book")
                .bookAuthor("Danke")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(book)
                .build();

        String libraryEventContent = objectMapper.writeValueAsString(libraryEvent);

        SettableListenableFuture<SendResult<Integer, String>> returnFuture = new SettableListenableFuture<>();

        Mockito
                .when(libraryEventProducer.sendLibraryEvent(Mockito.isA(LibraryEvent.class)))
                .thenReturn(returnFuture);

        mockMvc
                .perform(MockMvcRequestBuilders
                    .post("/v1/library-events")
                    .content(libraryEventContent)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpect(
                        MockMvcResultMatchers.status().isCreated()
                );
    }

    @Test
    public void postLibraryEvent_Failure4xx() throws Exception {
        Book book = Book.builder()
                .bookId(null)
                .bookName(null)
                .bookAuthor("Danke")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(book)
                .build();

        String libraryEventContent = objectMapper.writeValueAsString(libraryEvent);

        SettableListenableFuture<SendResult<Integer, String>> returnFuture = new SettableListenableFuture<>();

        Mockito
                .when(libraryEventProducer.sendLibraryEvent(Mockito.isA(LibraryEvent.class)))
                .thenReturn(returnFuture);

        String expectedResponseEntityBody = "{\"book.bookId\":\"must not be null\",\"book.bookName\":\"must not be blank\"}";

        mockMvc
                .perform(MockMvcRequestBuilders
                        .post("/v1/library-events")
                        .content(libraryEventContent)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpect(
                        MockMvcResultMatchers.status().is4xxClientError()
                )
                .andExpect(
                        MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(
                        MockMvcResultMatchers.content().string(expectedResponseEntityBody)
                );
    }
}
