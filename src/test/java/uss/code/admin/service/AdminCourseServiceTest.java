package uss.code.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.admin.domain.Admin;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncResult;
import uss.code.admin.dto.response.CourseSummaryResponse;
import uss.code.admin.fixture.AdminFixture;
import uss.code.admin.fixture.CourseSyncJobFixture;
import uss.code.admin.repository.AdminRepository;
import uss.code.admin.repository.CourseSyncJobRepository;
import uss.code.course.domain.Course;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;
import uss.code.course.repository.CourseRepository;
import uss.code.global.infra.IntegrationTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uss.code.admin.domain.SyncJobStatus.RUNNING;
import static uss.code.admin.domain.SyncJobStatus.SUCCESS;
import static uss.code.course.domain.CourseGrade.SOPHOMORE;
import static uss.code.course.domain.CourseTerm.SECOND;

@IntegrationTest
class AdminCourseServiceTest {

    private static final int TEST_ACADEMIC_YEAR = 2026;

    @Autowired
    private AdminCourseService adminCourseService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseSyncJobRepository courseSyncJobRepository;

    private Admin savedAdmin;

    @BeforeEach
    void setUp() {
        savedAdmin = adminRepository.save(AdminFixture.createAdmin());
    }

    @Nested
    class 적재_현황_조회_테스트 {

        @Test
        void 강의가_없으면_적재_학기가_비어있다() {
            //when
            final CourseSummaryResponse response = adminCourseService.getSummary();

            //then
            assertThat(response.semester()).isNull();
            assertThat(response.courseCount()).isZero();
            assertThat(response.scheduleCount()).isZero();
        }

        @Test
        void 적재된_학기와_건수를_반환한다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(course));
            courseRepository.save(course);

            //when
            final CourseSummaryResponse response = adminCourseService.getSummary();

            //then
            assertThat(response.semester().academicYear()).isEqualTo(TEST_ACADEMIC_YEAR);
            assertThat(response.semester().term()).isEqualTo(SECOND);
            assertThat(response.courseCount()).isEqualTo(1);
            assertThat(response.scheduleCount()).isEqualTo(1);
        }

        @Test
        void 강의_수는_폐강을_포함한다() {
            //given
            final Course active = CourseFixture.createCourseWithDetails(
                    "데이터구조", "Data Structure", "CSE2010", "CSE2010001", SOPHOMORE
            );
            final Course closed = CourseFixture.createCourseWithDetails(
                    "폐강과목", "Closed Course", "CSE2020", "CSE2020001", SOPHOMORE
            );
            closed.close();
            courseRepository.saveAll(List.of(active, closed));

            //when
            final CourseSummaryResponse response = adminCourseService.getSummary();

            //then
            assertThat(response.courseCount()).isEqualTo(2);
        }

        @Test
        void 이력이_없으면_최근_작업이_비어있다() {
            //when
            final CourseSummaryResponse response = adminCourseService.getSummary();

            //then
            assertThat(response.lastJob()).isNull();
            assertThat(response.runningJobId()).isNull();
        }

        @Test
        void 가장_최근에_시작한_작업을_반환한다() {
            //given
            courseSyncJobRepository.save(CourseSyncJobFixture.createSucceededJob(
                    savedAdmin,
                    LocalDateTime.now().minusDays(2),
                    SyncResult.of(1, 2, 3, 0)
            ));
            final CourseSyncJob latest = courseSyncJobRepository.save(CourseSyncJobFixture.createSucceededJob(
                    savedAdmin,
                    LocalDateTime.now().minusHours(1),
                    SyncResult.of(10, 20, 30, 0)
            ));

            //when
            final CourseSummaryResponse response = adminCourseService.getSummary();

            //then
            assertThat(response.lastJob().jobId()).isEqualTo(latest.getId());
            assertThat(response.lastJob().status()).isEqualTo(SUCCESS);
            assertThat(response.lastJob().createdCount()).isEqualTo(10);
            assertThat(response.lastJob().updatedCount()).isEqualTo(20);
            assertThat(response.lastJob().closedCount()).isEqualTo(30);
        }

        @Test
        void 성공하지_않은_작업의_건수는_비어있다() {
            //given
            courseSyncJobRepository.save(CourseSyncJobFixture.createFailedJob(
                    savedAdmin,
                    LocalDateTime.now(),
                    "연계 API 호출 실패"
            ));

            //when
            final CourseSummaryResponse response = adminCourseService.getSummary();

            //then
            assertThat(response.lastJob().createdCount()).isNull();
            assertThat(response.lastJob().updatedCount()).isNull();
            assertThat(response.lastJob().closedCount()).isNull();
        }

        @Test
        void 진행_중인_작업이_있으면_아이디를_반환한다() {
            //given
            final CourseSyncJob running = courseSyncJobRepository.save(
                    CourseSyncJobFixture.createRunningJob(savedAdmin)
            );

            //when
            final CourseSummaryResponse response = adminCourseService.getSummary();

            //then
            assertThat(response.runningJobId()).isEqualTo(running.getId());
            assertThat(response.lastJob().status()).isEqualTo(RUNNING);
        }
    }
}
