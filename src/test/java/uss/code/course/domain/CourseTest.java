package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;
import uss.code.global.infra.IntegrationTest;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
class CourseTest {

    @Nested
    class 수강_가능_판정_테스트 {

        @Test
        void 현재_수강인원이_정원보다_적으면_신청할_수_있다() {
            //given
            final Course course = CourseFixture.createCourse();

            //when
            final boolean registerable = course.isRegisterable();

            //then
            assertThat(registerable).isTrue();
        }

        @Test
        void 현재_수강인원이_정원과_같으면_신청할_수_없다() {
            //given
            final Course course = CourseFixture.createCourse(
                    "데이터구조", "Data Structure", "CSE2010", "CSE2010001",
                    CourseCollege.INFORMATION_TECHNOLOGY,
                    CourseDepartment.COMPUTER_ENGINEERING,
                    CourseClassification.MAJOR_CORE,
                    CourseArea.MAJOR_CORE,
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 50
            );

            //when
            final boolean registerable = course.isRegisterable();

            //then
            assertThat(registerable).isFalse();
        }
    }

    @Nested
    class 수업_길이_판정_테스트 {

        @Test
        void 시간표_중_하나라도_75분이면_75분_수업이다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "A03"));
            course.addCourseSchedule(CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "B02"));

            //when
            final boolean is75MinLesson = course.is75MinLesson();

            //then
            assertThat(is75MinLesson).isTrue();
        }

        @Test
        void 시간표가_모두_50분이면_75분_수업이_아니다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "A03"));
            course.addCourseSchedule(CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "C01"));

            //when
            final boolean is75MinLesson = course.is75MinLesson();

            //then
            assertThat(is75MinLesson).isFalse();
        }

        @Test
        void 시간표가_없으면_75분_수업이_아니다() {
            //given
            final Course course = CourseFixture.createCourse();

            //when
            final boolean is75MinLesson = course.is75MinLesson();

            //then
            assertThat(is75MinLesson).isFalse();
        }
    }

    @Nested
    class 연계_API_값_반영_테스트 {

        private static final int CHANGED_CREDITS = 4;
        private static final String CHANGED_TITLE_KR = "자료구조론";

        private CourseSnapshot snapshotOf(final Course course) {
            return snapshotBuilderOf(course).build();
        }

        private CourseSnapshot.CourseSnapshotBuilder snapshotBuilderOf(final Course course) {
            return CourseSnapshot.builder()
                    .academicYear(course.getAcademicYear())
                    .term(course.getTerm())
                    .titleKr(course.getTitleKr())
                    .titleEn(course.getTitleEn())
                    .courseCode(course.getCourseCode())
                    .haksuCode(course.getHaksuCode())
                    .college(course.getCollege())
                    .department(course.getDepartment())
                    .classificationCode(course.getClassificationCode())
                    .classificationName(course.getClassificationName())
                    .area(course.getArea())
                    .areaCode(course.getAreaCode())
                    .areaName(course.getAreaName())
                    .typeCode(course.getTypeCode())
                    .typeName(course.getTypeName())
                    .gradeCode(course.getGradeCode())
                    .gradeName(course.getGradeName())
                    .concentrationCode(course.getConcentrationCode())
                    .concentrationName(course.getConcentrationName())
                    .credits(course.getCredits())
                    .isEnglishCourse(course.isEnglishCourse())
                    .englishCode(course.getEnglishCode())
                    .englishName(course.getEnglishName())
                    .isHussCourse(course.isHussCourse());
        }

        @Test
        void 같은_값이면_변경_목록이_비어있다() {
            //given
            final Course course = CourseFixture.createCourse();

            //when
            final List<CourseFieldChange> changes = course.applyUpdate(snapshotOf(course));

            //then
            assertThat(changes).isEmpty();
        }

        @Test
        void 바뀐_필드만_변경_목록에_담긴다() {
            //given
            final Course course = CourseFixture.createCourse();
            final CourseSnapshot snapshot = snapshotBuilderOf(course).credits(CHANGED_CREDITS).build();

            //when
            final List<CourseFieldChange> changes = course.applyUpdate(snapshot);

            //then
            assertThat(changes)
                    .extracting(CourseFieldChange::field, CourseFieldChange::before, CourseFieldChange::after)
                    .containsExactly(tuple("credits", "3", "4"));
            assertThat(course.getCredits()).isEqualTo(CHANGED_CREDITS);
        }

        @Test
        void 여러_필드가_바뀌면_전부_담긴다() {
            //given
            final Course course = CourseFixture.createCourse();
            final CourseSnapshot snapshot = snapshotBuilderOf(course)
                    .credits(CHANGED_CREDITS)
                    .titleKr(CHANGED_TITLE_KR)
                    .build();

            //when
            final List<CourseFieldChange> changes = course.applyUpdate(snapshot);

            //then
            assertThat(changes)
                    .extracting(CourseFieldChange::field)
                    .containsExactlyInAnyOrder("titleKr", "credits");
        }

        @Test
        void 이수구분_명칭이_바뀌면_이수구분_한_필드로_담긴다() {
            //given
            final Course course = CourseFixture.createCourse();
            final CourseSnapshot snapshot = snapshotBuilderOf(course)
                    .classificationCode("41")
                    .classificationName("전공심화")
                    .build();

            //when
            final List<CourseFieldChange> changes = course.applyUpdate(snapshot);

            //then
            assertThat(changes)
                    .extracting(CourseFieldChange::field, CourseFieldChange::before, CourseFieldChange::after)
                    .containsExactly(tuple("classification", "전공핵심", "전공심화"));
            assertThat(course.getClassificationCode()).isEqualTo("41");
        }

        @Test
        void 정원과_현재_수강인원은_비교_대상이_아니다() {
            //given
            final Course course = CourseFixture.createCourse();
            final int maxCapacity = course.getMaxCapacity();
            final int currentEnrollment = course.getCurrentEnrollment();

            //when
            course.applyUpdate(snapshotBuilderOf(course).credits(CHANGED_CREDITS).build());

            //then
            assertThat(course.getMaxCapacity()).isEqualTo(maxCapacity);
            assertThat(course.getCurrentEnrollment()).isEqualTo(currentEnrollment);
        }
    }

    @Nested
    class 강의_상태_테스트 {

        @Test
        void 기본_상태는_개설이다() {
            //given
            final Course course = CourseFixture.createCourse();

            //when & then
            assertThat(course.isActive()).isTrue();
        }

        @Test
        void 폐강하면_개설_상태가_아니다() {
            //given
            final Course course = CourseFixture.createCourse();

            //when
            course.close();

            //then
            assertThat(course.isActive()).isFalse();
            assertThat(course.getStatus()).isEqualTo(CourseStatus.CLOSED);
        }

        @Test
        void 되살리면_다시_개설_상태가_된다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.close();

            //when
            course.reopen();

            //then
            assertThat(course.isActive()).isTrue();
        }
    }

    @Nested
    class 시간표_교체_테스트 {

        @Test
        void 교체하면_기존_시간표가_사라진다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(course));

            //when
            course.replaceSchedules(List.of(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.FRIDAY, LocalTime.of(13, 0), LocalTime.of(14, 15)
            )));

            //then
            assertThat(course.getSchedules()).hasSize(1);
            assertThat(course.getSchedules().get(0).getDayOfWeek()).isEqualTo(CourseDay.FRIDAY);
        }

        @Test
        void 빈_목록으로_교체하면_시간표가_없어진다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(course));

            //when
            course.replaceSchedules(List.of());

            //then
            assertThat(course.getSchedules()).isEmpty();
        }
    }
}
