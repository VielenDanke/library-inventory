package kz.danke.library.events.producer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.danke.library.events.producer.domain.LibraryEvent;
import kz.danke.library.events.producer.domain.LibraryEventType;
import kz.danke.library.events.producer.producer.LibraryEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/library-events")
public class LibraryEventsController {

    private final LibraryEventProducer libraryEventProducer;

    @Autowired
    public LibraryEventsController(LibraryEventProducer libraryEventProducer) {
        this.libraryEventProducer = libraryEventProducer;
    }

    @PostMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<LibraryEvent> postLibraryEvent(@RequestBody LibraryEvent libraryEvent) throws JsonProcessingException {
        libraryEvent.setLibraryEventType(LibraryEventType.NEW);

        libraryEventProducer.sendLibraryEvent(libraryEvent);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(libraryEvent);
    }
}
