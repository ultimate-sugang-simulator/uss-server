package uss.code.admin.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InuApiResponse<T>(
        String result,

        String resultMsg,

        Integer totalRecordCount,

        Integer totalpageSize,

        Integer pageRecordCount,

        Integer page,

        List<T> data
) {
    private static final String SUCCESS = "success";
    private static final int SINGLE_PAGE = 1;

    public boolean isSuccess() {
        return SUCCESS.equalsIgnoreCase(result);
    }

    public List<T> dataOrEmpty() {
        return data == null ? List.of() : data;
    }

    public int totalPagesOrSingle() {
        if (totalpageSize == null || totalpageSize < SINGLE_PAGE) {
            return SINGLE_PAGE;
        }

        return totalpageSize;
    }
}
