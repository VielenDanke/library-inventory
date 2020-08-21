package kz.danke.library.events.repository;

import kz.danke.library.events.entity.LibraryEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LibraryEventRepository extends JpaRepository<LibraryEvent, Integer> {

    @EntityGraph(attributePaths = {"book"})
    Optional<LibraryEvent> findByLibraryEventId(Integer id);
}
