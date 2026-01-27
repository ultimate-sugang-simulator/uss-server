package uss.code.auth.infra;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import uss.code.global.exception.domain.RestApiException;

import static uss.code.global.exception.domain.ExceptionCode.EMAIL_SENDING_FAILED;

@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender javaMailSender;

    public void sendVerificationCode(
            final String email,
            final String code
    ) {
        try {
            final MimeMessage message = javaMailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[궁극의 수강신청 시뮬레이터] 이메일 인증코드가 도착하였습니다.");
            helper.setText(EmailTemplateGenerator.generateVerificationCodeTemplate(code), true);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RestApiException(EMAIL_SENDING_FAILED);
        }
    }
}
