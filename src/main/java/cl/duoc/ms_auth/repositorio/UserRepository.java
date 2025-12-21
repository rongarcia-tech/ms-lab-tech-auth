package cl.duoc.ms_auth.repositorio;

import cl.duoc.ms_auth.entidades.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}

