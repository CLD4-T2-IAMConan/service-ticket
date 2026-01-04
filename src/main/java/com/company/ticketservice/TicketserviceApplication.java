package com.company.ticketservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.company.ticketservice", "com.company.sns"})
public class TicketserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketserviceApplication.class, args);
    }

    /**
     * SnsConfig를 TicketserviceApplication에 직접 추가
     * ComponentScan이 JAR 내부의 클래스를 스캔하지 못하는 문제를 해결하기 위해
     * SnsConfig를 직접 정의합니다.
     */
    @Configuration
    static class SnsConfig {
        @Value("${aws.region:ap-northeast-2}")
        private String region;

        @Bean
        public SnsClient snsClient() {
            return SnsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }

        @Bean
        public SqsClient sqsClient() {
            return SqsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }

        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper;
        }
    }
}
