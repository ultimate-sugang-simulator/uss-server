package uss.code.global.infra;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import uss.code.member.repository.InuMemberRepository;

@TestConfiguration
public class IntegrationTestConfig {

    @Bean
    public InuMemberRepository inuMemberRepository() {
        return Mockito.mock(InuMemberRepository.class);
    }
}
