package uss.code.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uss.code.auth.domain.EmailVerificationCode;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
}
