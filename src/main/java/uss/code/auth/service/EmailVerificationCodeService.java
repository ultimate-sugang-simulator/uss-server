package uss.code.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.auth.domain.EmailVerificationCode;
import uss.code.auth.dto.request.VerificationCodeSendRequest;
import uss.code.auth.dto.request.VerificationCodeVerifyRequest;
import uss.code.auth.infra.EmailSender;
import uss.code.auth.repository.EmailVerificationCodeRepository;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.repository.MemberRepository;

import java.time.LocalDateTime;

import static uss.code.global.exception.domain.BusinessExceptionCode.*;

@Service
@RequiredArgsConstructor
public class EmailVerificationCodeService {

    private final EmailSender emailSender;

    private final MemberRepository memberRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Transactional
    public void sendVerificationCode(final VerificationCodeSendRequest request) {
        if (validateUserExists(request.email()))
            throw new RestApiException(MEMBER_ALREADY_EXISTS);

        if(existsByEmailAndVerified(request.email(), false))
            throw new RestApiException(EMAIL_VERIFICATION_IN_PROGRESS);

        if(existsByEmailAndVerified(request.email(), true))
            throw new RestApiException(EMAIL_VERIFICATION_ALREADY_COMPLETED);

        final EmailVerificationCode emailVerificationCode = EmailVerificationCode.create(request.email());

        emailVerificationCodeRepository.save(emailVerificationCode);

        emailSender.sendVerificationCode(emailVerificationCode.getEmail(), emailVerificationCode.getCode());
    }

    @Transactional
    public void resendVerificationCode(final VerificationCodeSendRequest request) {
        EmailVerificationCode emailVerificationCode = emailVerificationCodeRepository.findByEmail(request.email())
                .orElseThrow(() -> new RestApiException(EMAIL_VERIFICATION_NOT_FOUND));

        if(emailVerificationCode.isVerified())
            throw new RestApiException(EMAIL_VERIFICATION_ALREADY_COMPLETED);

        if(emailVerificationCode.getResendCount() >= 5)
            throw new RestApiException(VERIFICATION_RESEND_LIMIT_EXCEEDED);

        emailVerificationCode.reissueCode();

        emailSender.sendVerificationCode(emailVerificationCode.getEmail(), emailVerificationCode.getCode());
    }

    @Transactional(noRollbackFor = RestApiException.class)
    public void verifyCode(final VerificationCodeVerifyRequest request) {
        EmailVerificationCode emailVerificationCode = emailVerificationCodeRepository.findByEmail(request.email())
                .orElseThrow(() -> new RestApiException(EMAIL_VERIFICATION_NOT_FOUND));

        if(emailVerificationCode.isVerified())
            throw new RestApiException(EMAIL_VERIFICATION_ALREADY_COMPLETED);

        if(LocalDateTime.now().isAfter(emailVerificationCode.getExpiresAt()))
            throw new RestApiException(VERIFICATION_CODE_EXPIRED);

        if(emailVerificationCode.getFailedCount() >= 5)
            throw new RestApiException(VERIFICATION_FAILED_LIMIT_EXCEEDED);

        if(!emailVerificationCode.getCode().equals(request.code())){
            emailVerificationCode.increaseFailedAttempts();
            emailVerificationCodeRepository.save(emailVerificationCode);

            throw new RestApiException(VERIFICATION_CODE_NOT_MATCH);
        }
        emailVerificationCode.markAsVerified();
    }

    private boolean validateUserExists(final String email) {
        return memberRepository.existsByEmail(email);
    }

    private boolean existsByEmailAndVerified(
            final String email,
            final boolean verified
    ) {
        return emailVerificationCodeRepository.existsByEmailAndVerified(email, verified);
    }
}
