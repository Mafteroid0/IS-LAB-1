package ru.itmo.routes.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;

/** Роли выводятся из настройки app.admins (список логинов администраторов через запятую). */
@Component
public class Roles {
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String USER = "ROLE_USER";

    private final Set<String> admins;

    public Roles(@Value("${app.admins:admin}") List<String> admins) {
        this.admins = Set.copyOf(admins.stream().map(String::strip).filter(name -> !name.isEmpty()).toList());
    }

    public List<GrantedAuthority> authorities(String username) {
        return AuthorityUtils.createAuthorityList(admins.contains(username) ? ADMIN : USER);
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(role -> ADMIN.equals(role.getAuthority()));
    }
}
