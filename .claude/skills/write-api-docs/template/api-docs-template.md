# API 문서 템플릿

```java
package uss.code.{domain}.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uss.code.auth.annotation.Auth;
import uss.code.global.exception.dto.response.ErrorResponse;

@Tag(name = "{Domain} API", description = "{도메인} 관련 API")
public interface {Controller}Docs {

    @Operation(summary = "{기능 요약}", description = "{상세 설명}<br>"
            + "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ {성공 메시지}"),
            @ApiResponse(responseCode = "404", description = "🚨 {에러 설명}",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "{에러명}",
                                            value = "{\"error\" : \"DOMAIN_CODE\", \"message\" : \"에러 메시지\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<{ResponseType}> methodName(@Auth final long memberId);
}
```
