package uss.code.auth.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import uss.code.global.exception.domain.AtInvalidException;
import uss.code.global.exception.domain.AtMissingException;
import uss.code.member.domain.Member;
import uss.code.member.repository.MemberRepository;

import static uss.code.global.exception.domain.AuthenticationExceptionCode.*;

@Component
@RequiredArgsConstructor
public class AtProvider {

    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public void validateToken(
            final String atToken,
            final long memberId
    ) {
        if(atToken == null){
            throw new AtMissingException(MISSING_ADMISSION_TOKEN);
        }

        final Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AtInvalidException(MEMBER_NOT_FOUND));

        final String savedToken = redisTemplate.opsForValue().get(member.getStudentId());

        if (savedToken == null) {
            throw new AtMissingException(ADMISSION_TOKEN_NOT_FOUND);
        }

        if(!atToken.equals(savedToken)) {
            throw new AtInvalidException(INVALID_ADMISSION_TOKEN);
        }

        deleteToken(member.getStudentId());
    }

    private void deleteToken(final String studentId) {
        redisTemplate.delete(studentId);
    }
}
