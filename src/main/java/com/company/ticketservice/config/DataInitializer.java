package com.company.ticketservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 개발/테스트 환경에서 필요한 기본 데이터를 자동으로 생성합니다.
 * - 테스트 유저 (user_id=1)
 * - 기본 카테고리들 (콘서트, 스포츠, 뮤지컬 등)
 */
@Slf4j
@Component
@Profile({"default", "dev"})  // 로컬 및 개발 환경에서만 실행
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            initializeTestUser();
            initializeCategories();
            log.info("테스트 데이터 초기화 완료");
        } catch (Exception e) {
            log.warn("테스트 데이터 초기화 중 오류 발생 (이미 존재할 수 있음): {}", e.getMessage());
        }
    }

    private void initializeTestUser() {
        // user_id=1인 테스트 유저가 없으면 생성
        String checkUserSql = "SELECT COUNT(*) FROM users WHERE user_id = 1";
        Integer userCount = jdbcTemplate.queryForObject(checkUserSql, Integer.class);

        if (userCount == null || userCount == 0) {
            String insertUserSql = """
                INSERT INTO users (user_id, email, password, name, role, status)
                VALUES (1, 'test@passit.com', 'password123', '테스트유저', 'USER', 'ACTIVE')
                """;
            jdbcTemplate.update(insertUserSql);
            log.info("테스트 유저 생성 완료 (user_id=1, email=test@passit.com)");
        } else {
            log.info("테스트 유저가 이미 존재합니다 (user_id=1)");
        }
    }

    private void initializeCategories() {
        // 기본 카테고리들이 없으면 생성
        String[][] categories = {
            {"1", "콘서트", "0"},
            {"2", "스포츠", "0"},
            {"3", "뮤지컬", "0"},
            {"4", "연극", "0"},
            {"5", "전시회", "0"},
            {"6", "기타", "0"}
        };

        for (String[] category : categories) {
            String categoryId = category[0];
            String categoryName = category[1];
            String depth = category[2];

            String checkCategorySql = "SELECT COUNT(*) FROM categories WHERE ticket_category_id = ?";
            Integer categoryCount = jdbcTemplate.queryForObject(checkCategorySql, Integer.class, categoryId);

            if (categoryCount == null || categoryCount == 0) {
                String insertCategorySql = """
                    INSERT INTO categories (ticket_category_id, large_name, name, depth, is_visible)
                    VALUES (?, ?, ?, ?, 'Y')
                    """;
                jdbcTemplate.update(insertCategorySql, categoryId, categoryName, categoryName, depth);
                log.info("카테고리 생성 완료: {} - {}", categoryId, categoryName);
            }
        }
    }
}
