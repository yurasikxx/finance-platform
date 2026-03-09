package by.bsuir.fp.repository;

import by.bsuir.fp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    // Поиск по GoogleID (для OAuth2)
    Optional<User> findByGoogleId(String googleId);
}