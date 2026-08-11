package uss.code.admin.infra;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uss.code.admin.dto.common.InuApiResponse;
import uss.code.admin.dto.common.InuCourseResponse;
import uss.code.admin.dto.common.InuTimetableResponse;
import uss.code.course.domain.CourseTerm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Component
public class InuCourseApiRestClient implements InuCourseApiClient {

    private static final String COURSE_API_PATH = "/A_MAP_COURSE_INFO";
    private static final String TIMETABLE_API_PATH = "/A_MAP_COURSE_TIMETABLE";

    private static final String AUTH_KEY_HEADER = "AUTH_KEY";
    private static final String PAGE_PARAM = "PAGE";
    private static final String MOD_DATE_PARAM = "MOD_DATE";
    private static final String YEAR_PARAM = "YEAR";
    private static final String TERM_CODE_PARAM = "TERM_CODE";

    private static final int FIRST_PAGE = 1;
    private static final int MAX_PAGE = 100;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final InuCourseApiProperties properties;

    public InuCourseApiRestClient(final InuCourseApiProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(createRequestFactory())
                .build();
        this.properties = properties;
    }

    private static ClientHttpRequestFactory createRequestFactory() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return requestFactory;
    }

    @Override
    public List<InuCourseResponse> fetchCourses(
            final int academicYear,
            final CourseTerm term
    ) {
        return fetchAllPages(COURSE_API_PATH, academicYear, term, new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<InuTimetableResponse> fetchTimetables(
            final int academicYear,
            final CourseTerm term
    ) {
        return fetchAllPages(TIMETABLE_API_PATH, academicYear, term, new ParameterizedTypeReference<>() {});
    }

    private <T> List<T> fetchAllPages(
            final String path,
            final int academicYear,
            final CourseTerm term,
            final ParameterizedTypeReference<InuApiResponse<T>> responseType
    ) {
        final List<T> collected = new ArrayList<>();

        int page = FIRST_PAGE;
        int totalPages = FIRST_PAGE;

        while (page <= totalPages && page <= MAX_PAGE) {
            final InuApiResponse<T> response = request(path, academicYear, term, page, responseType);

            if (response == null) {
                break;
            }

            if (!response.isSuccess()) {
                throw new InuCourseApiException(path + " 호출에 실패했습니다: " + response.resultMsg());
            }

            collected.addAll(response.dataOrEmpty());
            totalPages = response.totalPagesOrSingle();
            page++;
        }

        return collected;
    }

    private <T> InuApiResponse<T> request(
            final String path,
            final int academicYear,
            final CourseTerm term,
            final int page,
            final ParameterizedTypeReference<InuApiResponse<T>> responseType
    ) {
        try {
            return restClient.post()
                    .uri(uriBuilder -> uriBuilder.path(path)
                            .queryParam(PAGE_PARAM, page)
                            .queryParam(MOD_DATE_PARAM, properties.modDate())
                            .queryParam(YEAR_PARAM, academicYear)
                            .queryParam(TERM_CODE_PARAM, term.getCode())
                            .build())
                    .header(AUTH_KEY_HEADER, properties.authKey())
                    .retrieve()
                    .body(responseType);
        } catch (final Exception e) {
            return handleFailure(path, page, e);
        }
    }

    private <T> InuApiResponse<T> handleFailure(
            final String path,
            final int page,
            final Exception e
    ) {
        if (page > FIRST_PAGE) {
            log.warn("연계 API 다음 페이지를 해석하지 못해 수집을 멈춘다. path={}, page={}, message={}", path, page, e.getMessage());
            return null;
        }

        throw new InuCourseApiException(path + " 호출에 실패했습니다: " + e.getMessage());
    }
}
