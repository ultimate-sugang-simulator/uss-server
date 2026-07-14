# 테스트 코드 템플릿

## 통합 테스트

```java
package uss.code.{domain}.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.{ERROR_CODE};

@IntegrationTest
class {Target}Test {

    @Autowired
    private {TargetClass} target;

    @Autowired
    private {Repository} repository;

    @Nested
    class {기능}_테스트 {

        @BeforeEach
        void setUp() {
            // 테스트 데이터 준비 (repository.save/saveAll)
        }

        @Test
        void 정상_동작하면_성공한다() {
            //given

            //when
            ... result = target.method(param);

            //then
            assertThat(result)...;
        }

        @Test
        void 존재하지_않으면_예외가_발생한다() {
            //given

            //when & then
            // 예외 타입과 exceptionCode를 함께 검증한다 (코드 누락 시 회귀 감지 불가)
            assertThatThrownBy(() -> target.method(invalidParam))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", {ERROR_CODE});
        }
    }
}
```

## Fixture (ReflectionTestUtils)

```java
package uss.code.{domain}.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.{domain}.domain.{Entity};

public class {Entity}Fixture {

    public static {Entity} create{Entity}(...) {
        {Entity} entity = new {Entity}();
        ReflectionTestUtils.setField(entity, "{field}", value);
        // ... 필요한 필드마다 setField
        return entity;
    }
}
```
