package ferry.Auth.controllers;

import ferry.Auth.dtos.UserResponse;
import ferry.Auth.services.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                authService.login(authentication)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                authService.getCurrentUser(authentication)
        );
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            Authentication authentication
    ) {
        authService.deleteAccount(authentication);
        return ResponseEntity.noContent().build();
    }
}