package kz.danke.library.events.entity;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "book")
public class Book {

    @Id
    @Column(name = "book_id")
    private Integer bookId;

    @NotBlank
    @Column(name = "book_name")
    private String bookName;

    @NotBlank
    @Column(name = "book_author")
    private String bookAuthor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_event_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private LibraryEvent libraryEvent;
}
