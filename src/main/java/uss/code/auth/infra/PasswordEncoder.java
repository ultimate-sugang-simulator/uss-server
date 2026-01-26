package uss.code.auth.infra;

public interface PasswordEncoder {
    String encode(String password);
    boolean matches(
            String rawPassword,
            String encodedPassword
    );
}
