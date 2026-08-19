package uss.code.auth.infra;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class MemberPasswordEncoder {

    private static final int LOG_ROUNDS = 10;

    public String encode(final String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    public boolean matches(
            final String rawPassword,
            final String encodedPassword
    ) {
        try {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }
}
