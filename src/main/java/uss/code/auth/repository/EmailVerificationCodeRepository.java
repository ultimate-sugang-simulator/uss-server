package uss.code.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uss.code.auth.domain.EmailVerificationCode;

import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
    Optional<EmailVerificationCode> findByEmail(final String email);

    boolean existsByEmailAndVerified(
            final String email,
            final boolean verified
    );

    void deleteByEmail(final String email);
}
