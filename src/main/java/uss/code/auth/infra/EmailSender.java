package uss.code.auth.infra;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import uss.code.global.exception.domain.RestApiException;

import static uss.code.global.exception.domain.ExceptionCode.EMAIL_SENDING_FAILED;

@Log4j2
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
            helper.setSubject("[궁극의 수강신청 시뮬레이터] 인증코드가 도착하였습니다.");
            helper.setText(EmailTemplateGenerator.generateVerificationCodeTemplate(code), true);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            log.error(e.getMessage(), e);
            throw new RestApiException(EMAIL_SENDING_FAILED);
        }
    }
}
