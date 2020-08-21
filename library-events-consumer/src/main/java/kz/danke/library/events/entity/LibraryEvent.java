package kz.danke.library.events.entity;

import lombok.*;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "library_event")
public class LibraryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "library_event_id")
    private Integer libraryEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "library_event_type")
    private LibraryEventType libraryEventType;

    @NotNull
    @Valid
    @OneToOne(mappedBy = "libraryEvent", fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH
    })
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Book book;

    public void addBook(Book book) {
        this.book = book;
        book.setLibraryEvent(this);
    }
}
