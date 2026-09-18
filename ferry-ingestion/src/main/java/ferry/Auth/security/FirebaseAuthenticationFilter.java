package ferry.Auth.security;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import ferry.Auth.services.FirebaseUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@AllArgsConstructor
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {

            FirebaseToken decodedToken =
                    firebaseAuth.verifyIdToken(token);

            FirebaseUserPrincipal principal =
                    new FirebaseUserPrincipal(
                            decodedToken.getUid(),
                            decodedToken.getEmail(),
                            decodedToken.getName(),
                            decodedToken.isEmailVerified()
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

        } catch (Exception e) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write(
                    "{\"message\":\"Invalid Firebase token\"}"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}
