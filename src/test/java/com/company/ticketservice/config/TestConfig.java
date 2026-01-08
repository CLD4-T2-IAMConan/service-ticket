package com.company.ticketservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.mockito.Mockito.mock;

/**
 * 테스트 환경에서 AWS 클라이언트를 Mock으로 제공
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public SnsClient snsClient() {
        return mock(SnsClient.class);
    }

    @Bean
    @Primary
    public SqsClient sqsClient() {
        return mock(SqsClient.class);
    }
}

