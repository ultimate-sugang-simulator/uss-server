package uss.code.course.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.course.domain.Course;
import uss.code.course.dto.response.SearchedCourseResponse;
import uss.code.course.dto.response.SearchedCoursesResponse;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.repository.CourseRepository;
import uss.code.global.infra.MySqlIntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uss.code.course.domain.CourseGrade.ALL;
import static uss.code.course.domain.CourseGrade.FRESHMAN;
import static uss.code.course.domain.CourseGrade.SENIOR;

@MySqlIntegrationTest
class CourseServiceSearchTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @AfterEach
    void tearDown() {
        courseRepository.deleteAllInBatch();
    }

    @Nested
    class 키워드_검색_정렬_테스트 {

        private static final String KEYWORD = "정렬";

        @BeforeEach
        void setUp() {
            final List<Course> courses = List.of(
                    CourseFixture.createCourseWithDetails("정렬과 정렬 응용과 정렬", "Advanced Sorting", "SRCH005", "SRCH005001", SENIOR),
                    CourseFixture.createCourseWithDetails("자료구조", "Data Structure", "NONE001", "NONE001001", ALL),
                    CourseFixture.createCourseWithDetails("정렬 기초", "Sorting Basics", "SRCH001", "SRCH001001", ALL),
                    CourseFixture.createCourseWithDetails("정렬 입문", "Sorting Introduction", "SRCH004", "SRCH004001", FRESHMAN),
                    CourseFixture.createCourseWithDetails("정렬과 정렬 응용", "Sorting Applications", "SRCH002", "SRCH002001", ALL),
                    CourseFixture.createCourseWithDetails("운영체제", "Operating System", "NONE002", "NONE002001", FRESHMAN),
                    CourseFixture.createCourseWithDetails("정렬 입문", "Sorting Introduction", "SRCH003", "SRCH003001", FRESHMAN)
            );
            courseRepository.saveAll(courses);
        }

        @Test
        void 학년이_관련도보다_먼저_정렬된다() {
            //when
            final SearchedCoursesResponse response = courseService.searchCourses(KEYWORD);

            //then
            assertThat(response.searchedCourseResponses())
                    .extracting(SearchedCourseResponse::grade)
                    .containsExactly("전학년", "전학년", "1학년", "1학년", "4학년");
        }

        @Test
        void 같은_학년_안에서는_관련도가_높은_강의가_먼저_온다() {
            //when
            final SearchedCoursesResponse response = courseService.searchCourses(KEYWORD);

            //then
            final List<SearchedCourseResponse> searchedCourses = response.searchedCourseResponses();
            assertThat(searchedCourses.subList(0, 2))
                    .extracting(SearchedCourseResponse::haksuCode)
                    .containsExactly("SRCH002001", "SRCH001001");
        }

        @Test
        void 관련도가_같으면_학수번호_순으로_정렬된다() {
            //when
            final SearchedCoursesResponse response = courseService.searchCourses(KEYWORD);

            //then
            final List<SearchedCourseResponse> searchedCourses = response.searchedCourseResponses();
            assertThat(searchedCourses.subList(2, 4))
                    .extracting(SearchedCourseResponse::haksuCode)
                    .containsExactly("SRCH003001", "SRCH004001");
        }

        @Test
        void 검색어와_무관한_강의는_결과에_포함되지_않는다() {
            //when
            final SearchedCoursesResponse response = courseService.searchCourses(KEYWORD);

            //then
            assertThat(response.searchedCourseResponses())
                    .hasSize(5)
                    .extracting(SearchedCourseResponse::haksuCode)
                    .doesNotContain("NONE001001", "NONE002001");
        }
    }
}
