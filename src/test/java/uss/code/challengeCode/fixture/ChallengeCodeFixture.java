package uss.code.challengeCode.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.challengeCode.domain.ChallengeCode;
import uss.code.member.domain.Member;

public class ChallengeCodeFixture {

    public static ChallengeCode createChallengeCode(final Member member) {
        return createChallengeCode(member, "123456", 0);
    }

    public static ChallengeCode createChallengeCode(
            final Member member,
            final String code,
            final int failureCount
    ) {
        ChallengeCode challengeCode = new ChallengeCode();

        ReflectionTestUtils.setField(challengeCode, "member", member);
        ReflectionTestUtils.setField(challengeCode, "code", code);
        ReflectionTestUtils.setField(challengeCode, "failureCount", failureCount);

        return challengeCode;
    }

}
