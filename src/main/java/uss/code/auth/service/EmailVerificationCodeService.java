package uss.code.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.auth.domain.EmailVerificationCode;
import uss.code.auth.dto.request.VerificationCodeSendRequest;
import uss.code.auth.infra.EmailSender;
import uss.code.auth.repository.EmailVerificationCodeRepository;
import uss.code.global.exception.domain.ExceptionCode;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class EmailVerificationCodeService {

    private final EmailSender emailSender;

    private final MemberRepository memberRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Transactional
    public void sendVerificationCode(final VerificationCodeSendRequest request) {
        if (validateUserExists(request.email()))
            throw new RestApiException(ExceptionCode.MEMBER_ALREADY_EXISTS);

        final EmailVerificationCode emailVerificationCode = generateEmailVerificationCode(request.email());

        emailVerificationCodeRepository.save(emailVerificationCode);

        emailSender.sendVerificationCode(request.email(), emailVerificationCode.getCode());
    }

    private EmailVerificationCode generateEmailVerificationCode(final String email) {
        return EmailVerificationCode.create(email);
    }

    private boolean validateUserExists(final String email) {
        return memberRepository.existsByEmail(email);
    }
}
