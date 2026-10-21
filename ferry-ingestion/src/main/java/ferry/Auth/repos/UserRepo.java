package ferry.Auth.repos;


import ferry.Auth.enitities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    Optional<User> findByEmail(String email);

    boolean existsByFirebaseUid(String firebaseUid);

    boolean existsByEmail(String email);

    @Query("SELECT u.id FROM User u WHERE u.firebaseUid = :firebaseUid")
    Optional<Long> findIdByFirebaseUid(@Param("firebaseUid") String firebaseUid);
}