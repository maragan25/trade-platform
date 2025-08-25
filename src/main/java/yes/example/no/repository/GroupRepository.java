package yes.example.no.repository;

import java.util.*;
import yes.example.no.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByName(String name);
    List<Group> findByActiveTrue();
}
