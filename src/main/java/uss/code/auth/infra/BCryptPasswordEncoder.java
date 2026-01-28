package uss.code.auth.infra;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(final String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    @Override
    public boolean matches(
            final String rawPassword,
            final String encodedPassword
    ) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
