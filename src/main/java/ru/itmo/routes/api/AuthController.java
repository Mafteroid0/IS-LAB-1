package ru.itmo.routes.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import ru.itmo.routes.dto.Requests.Credentials;
import ru.itmo.routes.error.UnauthorizedException;
import ru.itmo.routes.service.AuthService;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {
    private final AuthService auth;
    private final AuthenticationManager authentication;
    private final SecurityContextRepository contexts;

    public AuthController(AuthService auth, AuthenticationManager authentication, SecurityContextRepository contexts) {
        this.auth = auth;
        this.authentication = authentication;
        this.contexts = contexts;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody Credentials input,
                                                        HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.status(201).body(signIn(auth.register(input), request, response));
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody Credentials input,
                                     HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication result = authentication.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(input.username, input.password));
            return signIn(result.getName(), request, response);
        } catch (AuthenticationException error) {
            throw new UnauthorizedException("Неверный логин или пароль");
        }
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication current) {
        return Map.of("username", current.getName());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /** Начинает новую сессию (защита от фиксации сессии) и сохраняет в ней контекст безопасности. */
    private Map<String, String> signIn(String username, HttpServletRequest request, HttpServletResponse response) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        request.getSession(true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                username, null, AuthorityUtils.NO_AUTHORITIES));
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return Map.of("username", username);
    }
}
