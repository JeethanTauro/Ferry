package ferry.Auth.services;

import com.google.firebase.auth.FirebaseAuth;
import ferry.Auth.dtos.UserResponse;
import ferry.Auth.enitities.User;
import ferry.Auth.enitities.UserRole;
import ferry.Auth.repos.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepo userRepository;
    private final FirebaseAuth firebaseAuth;

    public UserResponse login(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof FirebaseUserPrincipal principal)) {
            throw new RuntimeException("User is not authenticated");
        }

        User user = userRepository
                .findByFirebaseUid(principal.getFirebaseUid())
                .orElseGet(() -> createUser(principal));

        user.setEmailVerified(principal.isEmailVerified());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return UserResponse.from(user);
    }

    public UserResponse getCurrentUser(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof FirebaseUserPrincipal principal)) {
            throw new RuntimeException("User is not authenticated");
        }

        User user = userRepository
                .findByFirebaseUid(principal.getFirebaseUid())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return UserResponse.from(user);
    }

    //right now there could be a situation where it will delete in firebas but not in posgres or vice versa
    public void deleteAccount(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof FirebaseUserPrincipal principal)) {
            throw new RuntimeException("User is not authenticated");
        }

        User user = userRepository
                .findByFirebaseUid(principal.getFirebaseUid())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        try {
            firebaseAuth.deleteUser(principal.getFirebaseUid());
            userRepository.delete(user);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to delete user account", e
            );
        }
    }

    private User createUser(FirebaseUserPrincipal principal) {

        Instant now = Instant.now();

        User user = User.builder()
                .firebaseUid(principal.getFirebaseUid())
                .email(principal.getEmail())
                .name(principal.getName())
                .emailVerified(principal.isEmailVerified())
                .role(UserRole.USER)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return userRepository.save(user);
    }

    public Long getUserId(Authentication authentication) {
        if (authentication == null ||
                !(authentication.getPrincipal() instanceof FirebaseUserPrincipal principal)) {
            throw new RuntimeException("User is not authenticated");
        }

        User user = userRepository
                .findByFirebaseUid(principal.getFirebaseUid())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return user.getId();
    }
}