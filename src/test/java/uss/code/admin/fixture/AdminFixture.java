package uss.code.admin.fixture;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.test.util.ReflectionTestUtils;
import uss.code.admin.domain.Admin;
import uss.code.admin.domain.AdminRole;

import java.time.LocalDateTime;

public class AdminFixture {

    public static final String DEFAULT_LOGIN_ID = "test-admin";
    public static final String DEFAULT_PASSWORD = "test-admin-password";
    public static final String DEFAULT_NAME = "김학사";

    private static final int LOG_ROUNDS = 4;

    public static Admin createAdmin() {
        return createAdmin(DEFAULT_LOGIN_ID, DEFAULT_PASSWORD, DEFAULT_NAME);
    }

    public static Admin createAdmin(
            final String loginId,
            final String rawPassword,
            final String name
    ) {
        Admin admin = new Admin();

        ReflectionTestUtils.setField(admin, "loginId", loginId);
        ReflectionTestUtils.setField(admin, "password", encode(rawPassword));
        ReflectionTestUtils.setField(admin, "name", name);
        ReflectionTestUtils.setField(admin, "role", AdminRole.ADMIN);
        ReflectionTestUtils.setField(admin, "createdAt", LocalDateTime.now());

        return admin;
    }

    private static String encode(final String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(LOG_ROUNDS));
    }
}
