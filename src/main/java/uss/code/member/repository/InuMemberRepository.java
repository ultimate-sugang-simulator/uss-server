package uss.code.member.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "oracle.enabled", havingValue = "true")
public class InuMemberRepository {

    private final JdbcTemplate jdbcTemplate;

    public InuMemberRepository(@Qualifier("oracleJdbc") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean verifyInuMember(String studentId, String password) {
        String sql = "SELECT F_LOGIN_CHECK(?,?) FROM DUAL";

        try {
            String result = jdbcTemplate.queryForObject(sql, String.class, studentId, password);
            return "Y".equals(result);
        } catch (Exception e) {
            return false;
        }
    }
}
