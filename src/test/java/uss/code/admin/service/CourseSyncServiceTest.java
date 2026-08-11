package uss.code.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.admin.domain.Admin;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncResult;
import uss.code.admin.dto.request.SyncJobCreateRequest;
import uss.code.admin.dto.request.SyncPreflightRequest;
import uss.code.admin.dto.response.SyncChangeResponse;
import uss.code.admin.dto.response.SyncJobCreatedResponse;
import uss.code.admin.dto.response.SyncJobDetailResponse;
import uss.code.admin.dto.response.SyncJobResponse;
import uss.code.admin.dto.response.SyncPreflightResponse;
import uss.code.admin.fixture.AdminFixture;
import uss.code.admin.fixture.CourseSyncJobFixture;
import uss.code.admin.repository.AdminRepository;
import uss.code.admin.repository.CourseSyncJobRepository;
import uss.code.cart.domain.Cart;
import uss.code.cart.repository.CartRepository;
import uss.code.course.domain.Course;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;
import uss.code.course.repository.CourseRepository;
import uss.code.global.dto.response.PageResponse;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;
import uss.code.member.domain.AcademicStatus;
import uss.code.member.domain.Member;
import uss.code.member.domain.MemberCollege;
import uss.code.member.domain.MemberDepartment;
import uss.code.member.domain.MemberGrade;
import uss.code.member.fixture.MemberFixture;
import uss.code.member.repository.MemberRepository;
import uss.code.registration.domain.Registration;
import uss.code.registration.repository.RegistrationRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.admin.domain.SyncChangeType.CREATED;
import static uss.code.admin.domain.SyncJobStatus.RUNNING;
import static uss.code.admin.domain.SyncStrategy.INITIAL;
import static uss.code.admin.domain.SyncStrategy.REPLACE;
import static uss.code.admin.domain.SyncStrategy.UPSERT;
import static uss.code.course.domain.CourseGrade.SOPHOMORE;
import static uss.code.course.domain.CourseTerm.SECOND;
import static uss.code.course.domain.CourseTerm.SUMMER;
import static uss.code.global.exception.domain.ExceptionCode.SYNC_JOB_ALREADY_RUNNING;
import static uss.code.global.exception.domain.ExceptionCode.SYNC_JOB_NOT_FOUND;
import static uss.code.global.exception.domain.ExceptionCode.SYNC_STRATEGY_MISMATCH;

@IntegrationTest
class CourseSyncServiceTest {

    private static final int TEST_ACADEMIC_YEAR = 2026;
    private static final int FIRST_PAGE = 1;
    private static final long INVALID_JOB_ID = 999L;

    @Autowired
    private CourseSyncService courseSyncService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CourseSyncJobRepository courseSyncJobRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    private Admin savedAdmin;

    @BeforeEach
    void setUp() {
        savedAdmin = adminRepository.save(AdminFixture.createAdmin());
    }

    private Course saveCourseWithSchedule() {
        final Course course = CourseFixture.createCourse();
        course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(course));

        return courseRepository.save(course);
    }

    private Member saveMember() {
        return memberRepository.save(MemberFixture.createMember(
                "202012345",
                "홍길동",
                MemberCollege.INFORMATION_TECHNOLOGY,
                MemberDepartment.COMPUTER_ENGINEERING,
                MemberGrade.JUNIOR,
                AcademicStatus.ENROLLED,
                3.5
        ));
    }

    @Nested
    class 적재_전략_판정_테스트 {

        @Test
        void 강의가_없으면_최초_적재로_판정한다() {
            //when
            final SyncPreflightResponse response = courseSyncService.preflight(
                    new SyncPreflightRequest(TEST_ACADEMIC_YEAR, SECOND)
            );

            //then
            assertThat(response.strategy()).isEqualTo(INITIAL);
            assertThat(response.currentSemester()).isNull();
            assertThat(response.targetSemester().academicYear()).isEqualTo(TEST_ACADEMIC_YEAR);
            assertThat(response.targetSemester().term()).isEqualTo(SECOND);
        }

        @Test
        void 적재_학기와_대상_학기가_같으면_갱신으로_판정한다() {
            //given
            saveCourseWithSchedule();

            //when
            final SyncPreflightResponse response = courseSyncService.preflight(
                    new SyncPreflightRequest(TEST_ACADEMIC_YEAR, SECOND)
            );

            //then
            assertThat(response.strategy()).isEqualTo(UPSERT);
            assertThat(response.currentSemester().academicYear()).isEqualTo(TEST_ACADEMIC_YEAR);
            assertThat(response.currentSemester().term()).isEqualTo(SECOND);
        }

        @Test
        void 적재_학기와_대상_학기가_다르면_교체로_판정한다() {
            //given
            saveCourseWithSchedule();

            //when
            final SyncPreflightResponse response = courseSyncService.preflight(
                    new SyncPreflightRequest(TEST_ACADEMIC_YEAR, SUMMER)
            );

            //then
            assertThat(response.strategy()).isEqualTo(REPLACE);
        }

        @Test
        void 최초_적재의_삭제_예정_건수는_전부_0이다() {
            //when
            final SyncPreflightResponse response = courseSyncService.preflight(
                    new SyncPreflightRequest(TEST_ACADEMIC_YEAR, SECOND)
            );

            //then
            assertThat(response.deleteCounts().courses()).isZero();
            assertThat(response.deleteCounts().schedules()).isZero();
            assertThat(response.deleteCounts().carts()).isZero();
            assertThat(response.deleteCounts().registrations()).isZero();
        }

        @Test
        void 갱신의_삭제_예정_건수는_전부_0이다() {
            //given
            final Course course = saveCourseWithSchedule();
            final Member member = saveMember();
            cartRepository.save(Cart.create(member, course));

            //when
            final SyncPreflightResponse response = courseSyncService.preflight(
                    new SyncPreflightRequest(TEST_ACADEMIC_YEAR, SECOND)
            );

            //then
            assertThat(response.deleteCounts().courses()).isZero();
            assertThat(response.deleteCounts().carts()).isZero();
        }

        @Test
        void 교체의_삭제_예정_건수는_실제_건수를_센다() {
            //given
            final Course course = saveCourseWithSchedule();
            final Member member = saveMember();
            cartRepository.save(Cart.create(member, course));
            registrationRepository.save(Registration.create(member, course));

            //when
            final SyncPreflightResponse response = courseSyncService.preflight(
                    new SyncPreflightRequest(TEST_ACADEMIC_YEAR, SUMMER)
            );

            //then
            assertThat(response.deleteCounts().courses()).isEqualTo(1);
            assertThat(response.deleteCounts().schedules()).isEqualTo(1);
            assertThat(response.deleteCounts().carts()).isEqualTo(1);
            assertThat(response.deleteCounts().registrations()).isEqualTo(1);
        }

        @Test
        void 폐강된_강의도_삭제_예정_건수에_포함된다() {
            //given
            final Course course = saveCourseWithSchedule();
            course.close();
            courseRepository.save(course);

            //when
            final SyncPreflightResponse response = courseSyncService.preflight(
                    new SyncPreflightRequest(TEST_ACADEMIC_YEAR, SUMMER)
            );

            //then
            assertThat(response.deleteCounts().courses()).isEqualTo(1);
        }
    }

    @Nested
    class 동기화_작업_생성_테스트 {

        @Test
        void 전략이_일치하면_작업을_생성한다() {
            //given
            final SyncJobCreateRequest request = new SyncJobCreateRequest(TEST_ACADEMIC_YEAR, SECOND, INITIAL);

            //when
            final SyncJobCreatedResponse response = courseSyncService.createJob(savedAdmin.getId(), request);

            //then
            final CourseSyncJob job = courseSyncJobRepository.findById(response.jobId()).orElseThrow();
            assertThat(job.getStatus()).isEqualTo(RUNNING);
            assertThat(job.getStrategy()).isEqualTo(INITIAL);
            assertThat(job.getExecutedBy().getId()).isEqualTo(savedAdmin.getId());
        }

        @Test
        void 진행_중인_작업이_있으면_예외가_발생한다() {
            //given
            courseSyncJobRepository.save(CourseSyncJobFixture.createRunningJob(savedAdmin));
            final SyncJobCreateRequest request = new SyncJobCreateRequest(TEST_ACADEMIC_YEAR, SECOND, INITIAL);

            //when & then
            assertThatThrownBy(() -> courseSyncService.createJob(savedAdmin.getId(), request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", SYNC_JOB_ALREADY_RUNNING);
        }

        @Test
        void 재판정_결과가_요청한_전략과_다르면_예외가_발생한다() {
            //given
            saveCourseWithSchedule();
            final SyncJobCreateRequest request = new SyncJobCreateRequest(TEST_ACADEMIC_YEAR, SECOND, INITIAL);

            //when & then
            assertThatThrownBy(() -> courseSyncService.createJob(savedAdmin.getId(), request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", SYNC_STRATEGY_MISMATCH);
        }

        @Test
        void 전략이_어긋나면_작업을_만들지_않는다() {
            //given
            saveCourseWithSchedule();
            final SyncJobCreateRequest request = new SyncJobCreateRequest(TEST_ACADEMIC_YEAR, SECOND, INITIAL);

            //when
            assertThatThrownBy(() -> courseSyncService.createJob(savedAdmin.getId(), request))
                    .isInstanceOf(RestApiException.class);

            //then
            assertThat(courseSyncJobRepository.count()).isZero();
        }
    }

    @Nested
    class 작업_이력_조회_테스트 {

        @Test
        void 시작_시각_내림차순으로_반환한다() {
            //given
            courseSyncJobRepository.save(CourseSyncJobFixture.createJob(
                    savedAdmin, TEST_ACADEMIC_YEAR, SECOND, UPSERT, LocalDateTime.now().minusDays(3)
            ));
            final CourseSyncJob latest = courseSyncJobRepository.save(CourseSyncJobFixture.createJob(
                    savedAdmin, TEST_ACADEMIC_YEAR, SECOND, UPSERT, LocalDateTime.now().minusHours(1)
            ));

            //when
            final PageResponse<SyncJobResponse> response = courseSyncService.getJobs(FIRST_PAGE);

            //then
            assertThat(response.content()).hasSize(2);
            assertThat(response.content().get(0).jobId()).isEqualTo(latest.getId());
        }

        @Test
        void 페이지_크기는_10으로_고정이다() {
            //given
            for (int index = 0; index < 12; index++) {
                courseSyncJobRepository.save(CourseSyncJobFixture.createJob(
                        savedAdmin, TEST_ACADEMIC_YEAR, SECOND, UPSERT, LocalDateTime.now().minusMinutes(index)
                ));
            }

            //when
            final PageResponse<SyncJobResponse> response = courseSyncService.getJobs(FIRST_PAGE);

            //then
            assertThat(response.content()).hasSize(10);
            assertThat(response.page()).isEqualTo(1);
            assertThat(response.totalPages()).isEqualTo(2);
            assertThat(response.hasNextPage()).isTrue();
        }

        @Test
        void 마지막_페이지는_다음_페이지가_없다() {
            //given
            for (int index = 0; index < 12; index++) {
                courseSyncJobRepository.save(CourseSyncJobFixture.createJob(
                        savedAdmin, TEST_ACADEMIC_YEAR, SECOND, UPSERT, LocalDateTime.now().minusMinutes(index)
                ));
            }

            //when
            final PageResponse<SyncJobResponse> response = courseSyncService.getJobs(2);

            //then
            assertThat(response.content()).hasSize(2);
            assertThat(response.page()).isEqualTo(2);
            assertThat(response.hasNextPage()).isFalse();
        }

        @Test
        void 이력이_없으면_빈_목록을_반환한다() {
            //when
            final PageResponse<SyncJobResponse> response = courseSyncService.getJobs(FIRST_PAGE);

            //then
            assertThat(response.content()).isEmpty();
            assertThat(response.totalPages()).isZero();
            assertThat(response.hasNextPage()).isFalse();
        }
    }

    @Nested
    class 작업_상세_조회_테스트 {

        @Test
        void 진행_중이면_진행_단계를_반환하고_건수는_비어있다() {
            //given
            final CourseSyncJob job = courseSyncJobRepository.save(CourseSyncJobFixture.createRunningJob(savedAdmin));

            //when
            final SyncJobDetailResponse response = courseSyncService.getJob(job.getId());

            //then
            assertThat(response.status()).isEqualTo(RUNNING);
            assertThat(response.progress()).isNotNull();
            assertThat(response.finishedAt()).isNull();
            assertThat(response.durationSeconds()).isNull();
            assertThat(response.createdCount()).isNull();
        }

        @Test
        void 완료된_작업은_진행_단계가_비어있고_건수가_채워진다() {
            //given
            final CourseSyncJob job = courseSyncJobRepository.save(CourseSyncJobFixture.createSucceededJob(
                    savedAdmin,
                    LocalDateTime.now().minusMinutes(2),
                    SyncResult.of(1, 2, 3, 4)
            ));

            //when
            final SyncJobDetailResponse response = courseSyncService.getJob(job.getId());

            //then
            assertThat(response.progress()).isNull();
            assertThat(response.createdCount()).isEqualTo(1);
            assertThat(response.updatedCount()).isEqualTo(2);
            assertThat(response.closedCount()).isEqualTo(3);
            assertThat(response.warningCount()).isEqualTo(4);
            assertThat(response.durationSeconds()).isNotNull();
        }

        @Test
        void 실행한_관리자_이름을_반환한다() {
            //given
            final CourseSyncJob job = courseSyncJobRepository.save(CourseSyncJobFixture.createRunningJob(savedAdmin));

            //when
            final SyncJobDetailResponse response = courseSyncService.getJob(job.getId());

            //then
            assertThat(response.executedBy()).isEqualTo(AdminFixture.DEFAULT_NAME);
        }

        @Test
        void 존재하지_않는_작업이면_예외가_발생한다() {
            //when & then
            assertThatThrownBy(() -> courseSyncService.getJob(INVALID_JOB_ID))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", SYNC_JOB_NOT_FOUND);
        }
    }

    @Nested
    class 변경_항목_조회_테스트 {

        @Test
        void 존재하지_않는_작업이면_예외가_발생한다() {
            //when & then
            assertThatThrownBy(() -> courseSyncService.getJobDetails(INVALID_JOB_ID, CREATED, FIRST_PAGE))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", SYNC_JOB_NOT_FOUND);
        }

        @Test
        void 변경_항목이_없으면_빈_목록을_반환한다() {
            //given
            final CourseSyncJob job = courseSyncJobRepository.save(CourseSyncJobFixture.createRunningJob(savedAdmin));

            //when
            final PageResponse<SyncChangeResponse> response =
                    courseSyncService.getJobDetails(job.getId(), CREATED, FIRST_PAGE);

            //then
            assertThat(response.content()).isEmpty();
        }
    }

    @Nested
    class 폐강_강의_적재_현황_테스트 {

        @Test
        void 폐강_강의만_있어도_적재_학기가_잡힌다() {
            //given
            final Course course = CourseFixture.createCourseWithDetails(
                    "폐강과목", "Closed Course", "CSE2020", "CSE2020001", SOPHOMORE
            );
            course.close();
            courseRepository.save(course);

            //when
            final SyncPreflightResponse response = courseSyncService.preflight(
                    new SyncPreflightRequest(TEST_ACADEMIC_YEAR, SECOND)
            );

            //then
            assertThat(response.strategy()).isEqualTo(UPSERT);
        }
    }
}
