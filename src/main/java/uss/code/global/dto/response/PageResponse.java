package uss.code.global.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        @Schema(
                description = "현재 페이지 번호. 1부터 시작한다",
                example = "1"
        )
        int page,

        @Schema(
                description = "전체 페이지 수",
                example = "8"
        )
        int totalPages,

        @Schema(
                description = "다음 페이지 존재 여부",
                example = "true"
        )
        boolean hasNextPage,

        @Schema(description = "조회된 리소스 목록")
        List<T> content
) {
    private static final int PAGE_NUMBER_OFFSET = 1;

    public static <T> PageResponse<T> of(
            final Page<?> page,
            final List<T> content
    ) {
        return new PageResponse<>(
                page.getNumber() + PAGE_NUMBER_OFFSET,
                page.getTotalPages(),
                page.hasNext(),
                content
        );
    }
}
