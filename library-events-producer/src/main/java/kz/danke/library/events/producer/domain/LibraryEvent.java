package kz.danke.library.events.producer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryEvent {

    private Integer libraryEventId;
    private Book book;
    private LibraryEventType libraryEventType;
}
