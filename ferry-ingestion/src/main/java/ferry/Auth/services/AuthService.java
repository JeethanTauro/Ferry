package ferry.Auth.services;

import com.google.firebase.auth.FirebaseAuth;
import ferry.Auth.dtos.UserResponse;
import ferry.Auth.enitities.User;
import ferry.Auth.enitities.UserRole;
import ferry.exceptions.AccountDeletionException;
import ferry.exceptions.UnauthorizedException;
import ferry.exceptions.UserNotFoundException;
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

        FirebaseUserPrincipal principal = getPrincipal(authentication);

        User user = getOrCreateUser(principal);

        return UserResponse.from(user);
    }

    public UserResponse getCurrentUser(Authentication authentication) {

        FirebaseUserPrincipal principal = getPrincipal(authentication);

        User user = getOrCreateUser(principal);

        return UserResponse.from(user);
    }

    public Long getUserId(Authentication authentication) {

        FirebaseUserPrincipal principal = getPrincipal(authentication);

        User user = getOrCreateUser(principal);

        return user.getId();
    }

    public void deleteAccount(Authentication authentication) {

        FirebaseUserPrincipal principal = getPrincipal(authentication);

        User user = userRepository
                .findByFirebaseUid(principal.getFirebaseUid())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        try {

            /*
             * Firebase and PostgreSQL are separate systems,
             * so this operation cannot be fully atomic.
             *
             * Firebase is deleted first. If PostgreSQL deletion
             * fails afterwards, the local record may temporarily
             * remain and require reconciliation.
             */
            firebaseAuth.deleteUser(principal.getFirebaseUid());

            userRepository.delete(user);

        } catch (Exception e) {

            throw new AccountDeletionException(
                    "Failed to delete user account",
                    e
            );
        }
    }

    private User getOrCreateUser(
            FirebaseUserPrincipal principal) {

        User user = userRepository
                .findByFirebaseUid(principal.getFirebaseUid())
                .orElseGet(() -> createUser(principal));

        /*
         * These fields originate from Firebase and can be
         * synchronized whenever the user authenticates.
         *
         * Ferry-controlled fields such as role and active
         * are intentionally not modified here.
         */
        user.setEmail(principal.getEmail());
        user.setName(principal.getName());
        user.setEmailVerified(principal.isEmailVerified());
        user.setUpdatedAt(Instant.now());

        return userRepository.save(user);
    }

    private User createUser(
            FirebaseUserPrincipal principal) {

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

    private FirebaseUserPrincipal getPrincipal(
            Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal()
                        instanceof FirebaseUserPrincipal principal)) {

            throw new UnauthorizedException(
                    "User is not authenticated"
            );
        }

        return principal;
    }
}