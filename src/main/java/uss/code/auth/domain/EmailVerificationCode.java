package uss.code.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uss.code.global.infra.RandomCodeGenerator;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Table(name = "email_verification_codes")
@NoArgsConstructor(access = PROTECTED)
public class EmailVerificationCode {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private int failedCount;

    @Column(nullable = false)
    private int resendCount;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder(access = PRIVATE)
    private EmailVerificationCode(
            final String email,
            final String code
    ) {
        this.email = email;
        this.code = code;
        this.verified = false;
        this.failedCount = 0;
        this.resendCount = 0;
        this.expiresAt = LocalDateTime.now().plusMinutes(10);
    }

    public static EmailVerificationCode create(final String email){
        return EmailVerificationCode.builder()
                .email(email)
                .code(RandomCodeGenerator.generateCode())
                .build();
    }

    public void markAsVerified() {
        this.verified = true;
    }

    public void reissueCode() {
        this.code = RandomCodeGenerator.generateCode();
        this.failedCount = 0;
        this.resendCount++;
        this.expiresAt = LocalDateTime.now().plusMinutes(10);
    }

    public void increaseFailedAttempts() {
        this.failedCount++;
    }
}
