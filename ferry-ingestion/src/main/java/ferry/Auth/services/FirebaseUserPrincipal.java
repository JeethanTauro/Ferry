package ferry.Auth.services;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FirebaseUserPrincipal {

    private String firebaseUid;
    private String email;
    private String name;
    private boolean emailVerified;
}