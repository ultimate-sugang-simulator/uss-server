package uss.code.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;
import uss.code.member.domain.AcademicStatus;
import uss.code.member.domain.Member;
import uss.code.member.domain.MemberCollege;
import uss.code.member.domain.MemberDepartment;
import uss.code.member.domain.MemberGrade;
import uss.code.member.dto.response.MemberProfileResponse;
import uss.code.member.fixture.MemberFixture;
import uss.code.member.repository.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND;

@IntegrationTest
class MemberServiceTest {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;

    @Nested
    class 사용자의_헤더_정보를_조회할_때 {

        private static final String TEST_STUDENT_ID = "202012345";
        private static final String TEST_NAME = "홍길동";
        private static final MemberCollege TEST_COLLEGE = MemberCollege.INFORMATION_TECHNOLOGY;
        private static final MemberDepartment TEST_DEPARTMENT = MemberDepartment.COMPUTER_ENGINEERING;
        private static final MemberGrade TEST_GRADE = MemberGrade.JUNIOR;
        private static final AcademicStatus TEST_ACADEMIC_STATUS = AcademicStatus.ENROLLED;
        private static final double TEST_GPA = 3.5;

        private static final String EXPECTED_DEPARTMENT_NAME = "컴퓨터공학부";
        private static final String EXPECTED_GRADE_NAME = "3학년";
        private static final String EXPECTED_ACADEMIC_STATUS_NAME = "재학";

        long validMemberId;
        final long invalidMemberId = 999L;

        @BeforeEach
        void setUp() {
            final Member member = MemberFixture.createMember(
                    TEST_STUDENT_ID,
                    TEST_NAME,
                    TEST_COLLEGE,
                    TEST_DEPARTMENT,
                    TEST_GRADE,
                    TEST_ACADEMIC_STATUS,
                    TEST_GPA
            );

            memberRepository.save(member);

            validMemberId = member.getId();
        }

        @Test
        void 사용자_아이디가_유효하면_조회에_성공한다(){
            //given

            //when
            final MemberProfileResponse response = memberService.getProfile(validMemberId);

            //then
            assertThat(response.studentId()).isEqualTo(TEST_STUDENT_ID);
            assertThat(response.name()).isEqualTo(TEST_NAME);
            assertThat(response.department()).isEqualTo(EXPECTED_DEPARTMENT_NAME);
            assertThat(response.grade()).isEqualTo(EXPECTED_GRADE_NAME);
            assertThat(response.academicStatus()).isEqualTo(EXPECTED_ACADEMIC_STATUS_NAME);
        }

        @Test
        void 사용자_아이디가_유효하지_않으면_예외를_반환한다(){
            //given

            //when & then
            assertThatThrownBy(() -> memberService.getProfile(invalidMemberId))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND);
        }
    }
}