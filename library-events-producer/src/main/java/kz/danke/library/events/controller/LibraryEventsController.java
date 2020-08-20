package kz.danke.library.events.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.danke.library.events.domain.LibraryEvent;
import kz.danke.library.events.domain.LibraryEventType;
import kz.danke.library.events.producer.LibraryEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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
    public ResponseEntity<LibraryEvent> postLibraryEvent(@Valid @RequestBody LibraryEvent libraryEvent) throws JsonProcessingException {
        libraryEvent.setLibraryEventType(LibraryEventType.NEW);

        libraryEventProducer.sendLibraryEvent(libraryEvent);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(libraryEvent);
    }

    @PutMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<?> putLibraryEvent(@Valid @RequestBody LibraryEvent libraryEvent) throws JsonProcessingException {
        if (libraryEvent.getLibraryEventId() == null) {
            return ResponseEntity.badRequest().body("Please pass the Library Event ID");
        }
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);

        libraryEventProducer.sendLibraryEvent(libraryEvent);

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(libraryEvent);
    }
}
