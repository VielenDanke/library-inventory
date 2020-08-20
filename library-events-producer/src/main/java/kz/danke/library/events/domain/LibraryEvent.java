package kz.danke.library.events.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryEvent {

    private Integer libraryEventId;
    @NotNull
    @Valid
    private Book book;
    private LibraryEventType libraryEventType;
}
