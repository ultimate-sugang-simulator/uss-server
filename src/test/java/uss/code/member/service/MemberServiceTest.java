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
import uss.code.member.dto.request.DepartmentUpdateRequest;
import uss.code.member.dto.response.MemberProfileResponse;
import uss.code.member.fixture.MemberFixture;
import uss.code.member.repository.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;
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

    @Nested
    class 사용자의_학과를_수정할_때 {

        private static final String TEST_STUDENT_ID = "202054321";
        private static final String TEST_NAME = "김인천";
        private static final MemberGrade TEST_GRADE = MemberGrade.DEFAULT;
        private static final AcademicStatus TEST_ACADEMIC_STATUS = AcademicStatus.DEFAULT;
        private static final double TEST_GPA = 0.0;

        private static final String VALID_DEPARTMENT = "COMPUTER_ENGINEERING";
        private static final String INVALID_DEPARTMENT = "존재하지_않는_학과";

        long validMemberId;
        final long invalidMemberId = 999L;

        @BeforeEach
        void setUp() {
            final Member member = MemberFixture.createMember(
                    TEST_STUDENT_ID,
                    TEST_NAME,
                    MemberCollege.DEFAULT,
                    MemberDepartment.DEFAULT,
                    TEST_GRADE,
                    TEST_ACADEMIC_STATUS,
                    TEST_GPA
            );

            memberRepository.save(member);

            validMemberId = member.getId();
        }

        @Test
        void 유효한_학과가_들어오면_수정에_성공한다(){
            //given
            final DepartmentUpdateRequest request = new DepartmentUpdateRequest(VALID_DEPARTMENT);

            //when
            memberService.updateDepartment(validMemberId, request);

            //then
            final Member updatedMember = memberRepository.findById(validMemberId).orElseThrow();
            assertThat(updatedMember.getDepartment()).isEqualTo(MemberDepartment.COMPUTER_ENGINEERING);
        }

        @Test
        void 학과를_수정해도_단과대학은_바뀌지_않는다(){
            //given
            final DepartmentUpdateRequest request = new DepartmentUpdateRequest(VALID_DEPARTMENT);

            //when
            memberService.updateDepartment(validMemberId, request);

            //then
            final Member updatedMember = memberRepository.findById(validMemberId).orElseThrow();
            assertThat(updatedMember.getCollege()).isEqualTo(MemberCollege.DEFAULT);
        }

        @Test
        void 유효하지_않은_학과가_들어오면_예외를_반환한다(){
            //given
            final DepartmentUpdateRequest request = new DepartmentUpdateRequest(INVALID_DEPARTMENT);

            //when & then
            assertThatThrownBy(() -> memberService.updateDepartment(validMemberId, request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }

        @Test
        void 사용자_아이디가_유효하지_않으면_예외를_반환한다(){
            //given
            final DepartmentUpdateRequest request = new DepartmentUpdateRequest(VALID_DEPARTMENT);

            //when & then
            assertThatThrownBy(() -> memberService.updateDepartment(invalidMemberId, request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND);
        }
    }
}