package uss.code.cart.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CartedCoursesResponse(

        List<CartedCourseResponse> cartedCourseResponses
) {
    public static CartedCoursesResponse of(final List<CartedCourseResponse> cartedCourseResponses){
        return CartedCoursesResponse.builder()
                .cartedCourseResponses(cartedCourseResponses)
                .build();
    }
}
