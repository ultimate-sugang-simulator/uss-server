package uss.code.admin.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.admin.dto.request.SystemSemesterRequest;
import uss.code.admin.dto.response.SystemSemesterResponse;
import uss.code.admin.fixture.SystemSemesterFixture;
import uss.code.admin.repository.SystemSemesterRepository;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.course.domain.CourseTerm.SECOND;
import static uss.code.course.domain.CourseTerm.SUMMER;
import static uss.code.global.exception.domain.ExceptionCode.SYSTEM_SEMESTER_NOT_FOUND;

@IntegrationTest
class SystemSemesterServiceTest {

    private static final int TEST_ACADEMIC_YEAR = 2026;
    private static final int CHANGED_ACADEMIC_YEAR = 2027;

    @Autowired
    private SystemSemesterService systemSemesterService;

    @Autowired
    private SystemSemesterRepository systemSemesterRepository;

    @Nested
    class 표시_학기_조회_테스트 {

        @Test
        void 설정이_있으면_학년도와_학기를_반환한다() {
            //given
            systemSemesterRepository.save(SystemSemesterFixture.createSystemSemester(TEST_ACADEMIC_YEAR, SECOND));

            //when
            final SystemSemesterResponse response = systemSemesterService.getSystemSemester();

            //then
            assertThat(response.academicYear()).isEqualTo(TEST_ACADEMIC_YEAR);
            assertThat(response.term()).isEqualTo(SECOND);
        }

        @Test
        void 설정이_없으면_예외가_발생한다() {
            //when & then
            assertThatThrownBy(() -> systemSemesterService.getSystemSemester())
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", SYSTEM_SEMESTER_NOT_FOUND);
        }
    }

    @Nested
    class 표시_학기_변경_테스트 {

        @Test
        void 변경하면_바뀐_값을_반환한다() {
            //given
            systemSemesterRepository.save(SystemSemesterFixture.createSystemSemester(TEST_ACADEMIC_YEAR, SECOND));

            //when
            final SystemSemesterResponse response = systemSemesterService.changeSystemSemester(
                    new SystemSemesterRequest(CHANGED_ACADEMIC_YEAR, SUMMER)
            );

            //then
            assertThat(response.academicYear()).isEqualTo(CHANGED_ACADEMIC_YEAR);
            assertThat(response.term()).isEqualTo(SUMMER);
        }

        @Test
        void 변경한_값이_다음_조회에_반영된다() {
            //given
            systemSemesterRepository.save(SystemSemesterFixture.createSystemSemester(TEST_ACADEMIC_YEAR, SECOND));
            systemSemesterService.changeSystemSemester(new SystemSemesterRequest(CHANGED_ACADEMIC_YEAR, SUMMER));

            //when
            final SystemSemesterResponse response = systemSemesterService.getSystemSemester();

            //then
            assertThat(response.academicYear()).isEqualTo(CHANGED_ACADEMIC_YEAR);
            assertThat(response.term()).isEqualTo(SUMMER);
        }

        @Test
        void 행이_늘어나지_않는다() {
            //given
            systemSemesterRepository.save(SystemSemesterFixture.createSystemSemester(TEST_ACADEMIC_YEAR, SECOND));

            //when
            systemSemesterService.changeSystemSemester(new SystemSemesterRequest(CHANGED_ACADEMIC_YEAR, SUMMER));

            //then
            assertThat(systemSemesterRepository.count()).isEqualTo(1);
        }

        @Test
        void 설정이_없으면_예외가_발생한다() {
            //given
            final SystemSemesterRequest request = new SystemSemesterRequest(CHANGED_ACADEMIC_YEAR, SUMMER);

            //when & then
            assertThatThrownBy(() -> systemSemesterService.changeSystemSemester(request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", SYSTEM_SEMESTER_NOT_FOUND);
        }
    }
}
