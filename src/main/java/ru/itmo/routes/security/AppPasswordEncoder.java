package ru.itmo.routes.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itmo.routes.service.Passwords;

/** Сохраняет формат хешей PBKDF2 («итерации:соль:хеш»), чтобы существующие учётные записи продолжали работать. */
public class AppPasswordEncoder implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        return Passwords.hash(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return Passwords.matches(rawPassword.toString(), encodedPassword);
    }
}
