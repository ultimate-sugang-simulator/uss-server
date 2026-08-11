package uss.code.admin.infra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.admin.domain.Admin;
import uss.code.admin.domain.CourseSyncDetail;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncResult;
import uss.code.admin.domain.SyncStrategy;
import uss.code.admin.dto.common.InuCourseResponse;
import uss.code.admin.dto.common.InuTimetableResponse;
import uss.code.admin.fixture.AdminFixture;
import uss.code.admin.fixture.CourseSyncJobFixture;
import uss.code.admin.fixture.InuCourseApiFixture;
import uss.code.admin.repository.AdminRepository;
import uss.code.admin.repository.CourseSyncDetailRepository;
import uss.code.admin.repository.CourseSyncJobRepository;
import uss.code.cart.domain.Cart;
import uss.code.cart.repository.CartRepository;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseSchedule;
import uss.code.course.domain.CourseTerm;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;
import uss.code.course.repository.CourseRepository;
import uss.code.course.repository.CourseScheduleRepository;
import uss.code.global.infra.IntegrationTest;
import uss.code.member.domain.AcademicStatus;
import uss.code.member.domain.Member;
import uss.code.member.domain.MemberCollege;
import uss.code.member.domain.MemberDepartment;
import uss.code.member.domain.MemberGrade;
import uss.code.member.fixture.MemberFixture;
import uss.code.member.repository.MemberRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uss.code.admin.domain.SyncChangeType.CLOSED;
import static uss.code.admin.domain.SyncChangeType.CREATED;
import static uss.code.admin.domain.SyncChangeType.UPDATED;
import static uss.code.admin.domain.SyncChangeType.WARNING;
import static uss.code.admin.domain.SyncStrategy.INITIAL;
import static uss.code.admin.domain.SyncStrategy.REPLACE;
import static uss.code.admin.domain.SyncStrategy.UPSERT;
import static uss.code.course.domain.CourseGrade.SOPHOMORE;
import static uss.code.course.domain.CourseStatus.ACTIVE;
import static uss.code.course.domain.CourseTerm.SECOND;
import static uss.code.course.domain.CourseTerm.SUMMER;

@IntegrationTest
class CourseSyncApplierTest {

    private static final int TEST_ACADEMIC_YEAR = 2026;
    private static final int DEFAULT_MAX_CAPACITY = 100;

    private static final String HAKSU_CODE = "CSE2010001";
    private static final String OTHER_HAKSU_CODE = "CSE2020001";
    private static final String TITLE_KR = "데이터구조";

    @Autowired
    private CourseSyncApplier courseSyncApplier;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CourseSyncJobRepository courseSyncJobRepository;

    @Autowired
    private CourseSyncDetailRepository courseSyncDetailRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseScheduleRepository courseScheduleRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CartRepository cartRepository;

    private Admin savedAdmin;

    @BeforeEach
    void setUp() {
        savedAdmin = adminRepository.save(AdminFixture.createAdmin());
    }

    private CourseSyncJob saveJob(final SyncStrategy strategy) {
        return saveJob(strategy, SECOND);
    }

    private CourseSyncJob saveJob(
            final SyncStrategy strategy,
            final CourseTerm term
    ) {
        return courseSyncJobRepository.save(
                CourseSyncJobFixture.createRunningJob(savedAdmin, TEST_ACADEMIC_YEAR, term, strategy)
        );
    }

    private List<CourseSyncDetail> findDetails(final CourseSyncJob job) {
        return courseSyncDetailRepository.findAll().stream()
                .filter(detail -> detail.getJob().getId().equals(job.getId()))
                .toList();
    }

    @Nested
    class 최초_적재_테스트 {

        @Test
        void 수집한_강의를_전부_생성한다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);
            final List<InuCourseResponse> courses = List.of(
                    InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR),
                    InuCourseApiFixture.createCourse(OTHER_HAKSU_CODE, "운영체제")
            );

            //when
            final SyncResult result = courseSyncApplier.apply(job.getId(), courses, List.of());

            //then
            assertThat(result.createdCount()).isEqualTo(2);
            assertThat(result.updatedCount()).isZero();
            assertThat(result.closedCount()).isZero();
            assertThat(courseRepository.count()).isEqualTo(2);
        }

        @Test
        void 생성된_강의는_개설_상태이고_기본_정원을_가진다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of()
            );

            //then
            final Course created = courseRepository.findAll().get(0);
            assertThat(created.getStatus()).isEqualTo(ACTIVE);
            assertThat(created.getMaxCapacity()).isEqualTo(DEFAULT_MAX_CAPACITY);
            assertThat(created.getCurrentEnrollment()).isZero();
        }

        @Test
        void 시간표를_함께_적재한다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of(InuCourseApiFixture.createTimetable(HAKSU_CODE))
            );

            //then
            assertThat(courseScheduleRepository.count()).isEqualTo(1);
        }

        @Test
        void 강의실은_건물과_호실만_남긴다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of(InuCourseApiFixture.createTimetable(HAKSU_CODE, "1", "제7호관 정보기술대학-407 강의실"))
            );

            //then
            final CourseSchedule schedule = courseScheduleRepository.findAll().get(0);
            assertThat(schedule.getClassroom()).isEqualTo("07-407");
        }

        @Test
        void 강좌_목록에_없는_학수번호의_시간표는_버린다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of(
                            InuCourseApiFixture.createTimetable(HAKSU_CODE),
                            InuCourseApiFixture.createTimetable("UNKNOWN999")
                    )
            );

            //then
            assertThat(courseScheduleRepository.count()).isEqualTo(1);
        }

        @Test
        void 생성_항목을_이력에_남긴다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of()
            );

            //then
            final List<CourseSyncDetail> details = findDetails(job);
            assertThat(details).hasSize(1);
            assertThat(details.get(0).getChangeType()).isEqualTo(CREATED);
            assertThat(details.get(0).getHaksuCode()).isEqualTo(HAKSU_CODE);
        }

        @Test
        void 학년_명칭에_학년을_붙인다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of()
            );

            //then
            assertThat(courseRepository.findAll().get(0).getGradeName()).isEqualTo("2학년");
        }
    }

    @Nested
    class 교체_적재_테스트 {

        @Test
        void 기존_학기_데이터를_지우고_새로_적재한다() {
            //given
            final Course existing = CourseFixture.createCourse();
            existing.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(existing));
            courseRepository.save(existing);

            final CourseSyncJob job = saveJob(REPLACE, SUMMER);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(OTHER_HAKSU_CODE, "운영체제")),
                    List.of()
            );

            //then
            assertThat(result.createdCount()).isEqualTo(1);
            assertThat(result.closedCount()).isZero();
            assertThat(courseRepository.count()).isEqualTo(1);
            assertThat(courseRepository.findAll().get(0).getHaksuCode()).isEqualTo(OTHER_HAKSU_CODE);
        }

        @Test
        void 기존_학기의_장바구니도_함께_지운다() {
            //given
            final Course existing = courseRepository.save(CourseFixture.createCourse());
            final Member member = memberRepository.save(MemberFixture.createMember(
                    "202012345",
                    "홍길동",
                    MemberCollege.INFORMATION_TECHNOLOGY,
                    MemberDepartment.COMPUTER_ENGINEERING,
                    MemberGrade.JUNIOR,
                    AcademicStatus.ENROLLED,
                    3.5
            ));
            cartRepository.save(Cart.create(member, existing));

            final CourseSyncJob job = saveJob(REPLACE, SUMMER);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(OTHER_HAKSU_CODE, "운영체제")),
                    List.of()
            );

            //then
            assertThat(cartRepository.count()).isZero();
        }
    }

    @Nested
    class 갱신_적재_테스트 {

        @Test
        void 없던_강의는_생성한다() {
            //given
            courseRepository.save(CourseFixture.createCourse());
            final CourseSyncJob job = saveJob(UPSERT);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(
                            InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR),
                            InuCourseApiFixture.createCourse(OTHER_HAKSU_CODE, "운영체제")
                    ),
                    List.of()
            );

            //then
            assertThat(result.createdCount()).isEqualTo(1);
            assertThat(courseRepository.count()).isEqualTo(2);
        }

        @Test
        void 바뀐_값이_있으면_수정으로_기록한다() {
            //given
            courseRepository.save(CourseFixture.createCourse());
            final CourseSyncJob job = saveJob(UPSERT);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourseWithCredits(HAKSU_CODE, TITLE_KR, 4)),
                    List.of()
            );

            //then
            assertThat(result.updatedCount()).isEqualTo(1);

            final List<CourseSyncDetail> details = findDetails(job);
            assertThat(details).hasSize(1);
            assertThat(details.get(0).getChangeType()).isEqualTo(UPDATED);
            assertThat(details.get(0).getChangedFields())
                    .extracting("field", "beforeValue", "afterValue")
                    .contains(tuple("credits", "3", "4"));
        }

        @Test
        void 이수구분이_바뀌면_원문_명칭으로_기록한다() {
            //given
            courseRepository.save(CourseFixture.createCourse());
            final CourseSyncJob job = saveJob(UPSERT);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourseWithClassification(HAKSU_CODE, TITLE_KR, "전공심화")),
                    List.of()
            );

            //then
            assertThat(findDetails(job).get(0).getChangedFields())
                    .extracting("field", "beforeValue", "afterValue")
                    .contains(tuple("classification", "전공핵심", "전공심화"));
        }

        @Test
        void 바뀐_값이_없으면_아무것도_기록하지_않는다() {
            //given
            courseRepository.save(CourseFixture.createCourse());
            final CourseSyncJob job = saveJob(UPSERT);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of()
            );

            //then
            assertThat(result.createdCount()).isZero();
            assertThat(result.updatedCount()).isZero();
            assertThat(result.closedCount()).isZero();
            assertThat(findDetails(job)).isEmpty();
        }

        @Test
        void 수집되지_않은_강의는_폐강한다() {
            //given
            courseRepository.save(CourseFixture.createCourse());
            final CourseSyncJob job = saveJob(UPSERT);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(OTHER_HAKSU_CODE, "운영체제")),
                    List.of()
            );

            //then
            assertThat(result.closedCount()).isEqualTo(1);

            final Course closed = courseRepository.findAll().stream()
                    .filter(course -> course.getHaksuCode().equals(HAKSU_CODE))
                    .findFirst()
                    .orElseThrow();
            assertThat(closed.isActive()).isFalse();
        }

        @Test
        void 폐강된_강의는_지우지_않는다() {
            //given
            courseRepository.save(CourseFixture.createCourse());
            final CourseSyncJob job = saveJob(UPSERT);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(OTHER_HAKSU_CODE, "운영체제")),
                    List.of()
            );

            //then
            assertThat(courseRepository.count()).isEqualTo(2);
        }

        @Test
        void 이미_폐강된_강의는_다시_폐강하지_않는다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.close();
            courseRepository.save(course);
            final CourseSyncJob job = saveJob(UPSERT);

            //when
            final SyncResult result = courseSyncApplier.apply(job.getId(), List.of(), List.of());

            //then
            assertThat(result.closedCount()).isZero();
            assertThat(findDetails(job)).isEmpty();
        }

        @Test
        void 폐강된_강의가_다시_수집되면_되살리고_상태_변경을_남긴다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.close();
            courseRepository.save(course);
            final CourseSyncJob job = saveJob(UPSERT);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of()
            );

            //then
            assertThat(result.updatedCount()).isEqualTo(1);
            assertThat(courseRepository.findAll().get(0).isActive()).isTrue();

            final List<CourseSyncDetail> details = findDetails(job);
            assertThat(details.get(0).getChangedFields())
                    .extracting("field", "beforeValue", "afterValue")
                    .contains(tuple("status", "CLOSED", "ACTIVE"));
        }

        @Test
        void 시간표가_바뀌면_통째로_교체하고_한_필드로_기록한다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(course));
            courseRepository.save(course);
            final CourseSyncJob job = saveJob(UPSERT);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of(InuCourseApiFixture.createTimetable(HAKSU_CODE, "2", "제15호관 인문대학-113 강의실"))
            );

            //then
            assertThat(result.updatedCount()).isEqualTo(1);

            final List<CourseSyncDetail> details = findDetails(job);
            assertThat(details.get(0).getChangedFields())
                    .extracting("field")
                    .containsExactly("schedule");
        }

        @Test
        void 정원과_현재_수강인원은_바꾸지_않는다() {
            //given
            courseRepository.save(CourseFixture.createCourse());
            final int originalMaxCapacity = courseRepository.findAll().get(0).getMaxCapacity();
            final int originalEnrollment = courseRepository.findAll().get(0).getCurrentEnrollment();

            final CourseSyncJob job = saveJob(UPSERT);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourseWithCredits(HAKSU_CODE, TITLE_KR, 4)),
                    List.of()
            );

            //then
            final Course updated = courseRepository.findAll().get(0);
            assertThat(updated.getMaxCapacity()).isEqualTo(originalMaxCapacity);
            assertThat(updated.getCurrentEnrollment()).isEqualTo(originalEnrollment);
        }

        @Test
        void 갱신은_장바구니를_지우지_않는다() {
            //given
            final Course existing = courseRepository.save(CourseFixture.createCourse());
            final Member member = memberRepository.save(MemberFixture.createMember(
                    "202012345",
                    "홍길동",
                    MemberCollege.INFORMATION_TECHNOLOGY,
                    MemberDepartment.COMPUTER_ENGINEERING,
                    MemberGrade.JUNIOR,
                    AcademicStatus.ENROLLED,
                    3.5
            ));
            cartRepository.save(Cart.create(member, existing));

            final CourseSyncJob job = saveJob(UPSERT);

            //when
            courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of()
            );

            //then
            assertThat(cartRepository.count()).isEqualTo(1);
        }
    }

    @Nested
    class 경고_처리_테스트 {

        @Test
        void 등록되지_않은_학과_코드면_강의를_건너뛰고_경고로_남긴다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourseWithDepartmentCode(HAKSU_CODE, TITLE_KR, "0000999")),
                    List.of()
            );

            //then
            assertThat(result.createdCount()).isZero();
            assertThat(result.warningCount()).isEqualTo(1);
            assertThat(courseRepository.count()).isZero();

            final List<CourseSyncDetail> details = findDetails(job);
            assertThat(details.get(0).getChangeType()).isEqualTo(WARNING);
            assertThat(details.get(0).getReason()).isEqualTo("미등록 학과 코드: 0000999");
        }

        @Test
        void 등록되지_않은_이수영역_코드면_경고로_남긴다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourseWithAreaCode(HAKSU_CODE, TITLE_KR, "999")),
                    List.of()
            );

            //then
            assertThat(result.warningCount()).isEqualTo(1);
            assertThat(findDetails(job).get(0).getReason()).isEqualTo("미등록 이수영역 코드: 999");
        }

        @Test
        void 경고가_있어도_나머지_강의는_적재한다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(
                            InuCourseApiFixture.createCourseWithDepartmentCode(HAKSU_CODE, TITLE_KR, "0000999"),
                            InuCourseApiFixture.createCourse(OTHER_HAKSU_CODE, "운영체제")
                    ),
                    List.of()
            );

            //then
            assertThat(result.createdCount()).isEqualTo(1);
            assertThat(result.warningCount()).isEqualTo(1);
        }

        @Test
        void 등록되지_않은_요일_코드면_그_시간표만_버리고_강의는_적재한다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of(InuCourseApiFixture.createTimetable(HAKSU_CODE, "9", "제7호관 정보기술대학-407"))
            );

            //then
            assertThat(result.createdCount()).isEqualTo(1);
            assertThat(result.warningCount()).isEqualTo(1);
            assertThat(courseScheduleRepository.count()).isZero();
            assertThat(findDetails(job))
                    .filteredOn(detail -> detail.getChangeType() == WARNING)
                    .extracting(CourseSyncDetail::getReason)
                    .containsExactly("미등록 요일 코드: 9");
        }

        @Test
        void 해석할_수_없는_교시_시각이면_경고로_남긴다() {
            //given
            final CourseSyncJob job = saveJob(INITIAL);

            //when
            final SyncResult result = courseSyncApplier.apply(
                    job.getId(),
                    List.of(InuCourseApiFixture.createCourse(HAKSU_CODE, TITLE_KR)),
                    List.of(InuCourseApiFixture.createTimetableWithTime(HAKSU_CODE, "아홉시", "10:15"))
            );

            //then
            assertThat(result.warningCount()).isEqualTo(1);
            assertThat(courseScheduleRepository.count()).isZero();
        }
    }

    @Nested
    class 폐강_이력_테스트 {

        @Test
        void 폐강_항목을_이력에_남긴다() {
            //given
            courseRepository.save(CourseFixture.createCourseWithDetails(
                    TITLE_KR, "Data Structure", "CSE2010", HAKSU_CODE, SOPHOMORE
            ));
            final CourseSyncJob job = saveJob(UPSERT);

            //when
            courseSyncApplier.apply(job.getId(), List.of(), List.of());

            //then
            final List<CourseSyncDetail> details = findDetails(job);
            assertThat(details).hasSize(1);
            assertThat(details.get(0).getChangeType()).isEqualTo(CLOSED);
            assertThat(details.get(0).getHaksuCode()).isEqualTo(HAKSU_CODE);
            assertThat(details.get(0).getCourseName()).isEqualTo(TITLE_KR);
        }
    }
}
