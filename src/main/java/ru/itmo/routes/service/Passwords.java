package ru.itmo.routes.service;

import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
    private static final int ITERATIONS = 210000;

    private Passwords() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean matches(String password, String encoded) {
        String[] parts = encoded.split(":");
        byte[] actual = derive(password, Base64.getDecoder().decode(parts[1]), Integer.parseInt(parts[0]));
        return MessageDigest.isEqual(actual, Base64.getDecoder().decode(parts[2]));
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Недоступен алгоритм хеширования", error);
        } finally {
            spec.clearPassword();
        }
    }
}
