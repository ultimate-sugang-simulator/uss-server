package uss.code;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import uss.code.global.infra.IntegrationTestConfig;

@SpringBootTest
@Import(IntegrationTestConfig.class)
class UssServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
