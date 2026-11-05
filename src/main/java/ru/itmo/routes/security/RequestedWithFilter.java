package ru.itmo.routes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

/**
 * Защита от CSRF: запросы API, изменяющие данные, должны содержать заголовок X-Requested-With,
 * который браузер не позволяет добавить кросс-доменной форме.
 */
class RequestedWithFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Requested-With";
    static final String VALUE = "RouteLab";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final ObjectMapper mapper;

    RequestedWithFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod())
                || !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (VALUE.equals(request.getHeader(HEADER))) {
            chain.doFilter(request, response);
        } else {
            SecurityConfig.write(response, mapper, 403, "Запрос не прошёл проверку: отсутствует X-Requested-With");
        }
    }
}
