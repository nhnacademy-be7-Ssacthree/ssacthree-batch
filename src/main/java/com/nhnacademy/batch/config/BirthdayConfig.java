package com.nhnacademy.batch.config;

import com.nhnacademy.batch.domain.Member;
import com.nhnacademy.batch.domain.mapper.MemberCustomRowMapper;
import com.nhnacademy.batch.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BirthdayConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final DataSource dataSource; // 데이터베이스 연결 정보 설정


    @Bean
    public Job birthdayJob() {
        return new JobBuilder("birthdayJob", jobRepository)
                .start(birthdayStep())
                .build();
    }

    @Bean
    public Step birthdayStep() {
        return new StepBuilder("BirthdayStep", jobRepository)
                .<Member, Member> chunk(10,transactionManager)
                .reader(birthdayReader())
                .writer(birthdayWriter())
                .build();
    }

    @Bean
    public JdbcPagingItemReader<Member> birthdayReader() {
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("customer_id", Order.ASCENDING);

        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT customer_id");
        queryProvider.setFromClause("FROM member m");
        queryProvider.setWhereClause("""
        WHERE EXTRACT(MONTH FROM m.member_birthdate) = EXTRACT(MONTH FROM CURRENT_DATE())
        AND EXTRACT(DAY FROM m.member_birthdate) = EXTRACT(DAY FROM CURRENT_DATE())
        AND m.member_status = 'ACTIVE'
        """);
        queryProvider.setSortKeys(sortKeys);

        return new JdbcPagingItemReaderBuilder<Member>()
                .name("birthdayReader") // ItemReader 이름 설정
                .dataSource(dataSource) // DataSource를 설정하여 데이터베이스와의 연결 관리
                .pageSize(50) // 한 번에 읽어올 데이터의 개수를 설정하여 성능 최적화
                .queryProvider(queryProvider) // PagingQueryProvider를 설정하여 페이지 단위의 데이터를 가져오는 쿼리 제공
                .rowMapper(new MemberCustomRowMapper()) // SQL 쿼리 결과를 Member 객체로 변환하기
                .build();
    }

    @Bean
    @SuppressWarnings("squid:S1854")
    public ItemWriter<Member> birthdayWriter() {
        return new ItemWriter<>() {
            @Override
            public void write(Chunk<? extends Member> items) throws Exception {
                String couponQuery = """
                    SELECT c.coupon_id, c.coupon_effective_period, c.coupon_effective_period_unit, c.coupon_expired_at
                    FROM coupon c
                    INNER JOIN coupon_rule cr ON c.coupon_rule_id = cr.coupon_rule_id
                    WHERE cr.coupon_rule_name LIKE '%생일%'
                    AND cr.coupon_is_used
                    AND (c.coupon_expired_at > CURRENT_DATE() OR c.coupon_expired_at IS NULL)
                    LIMIT 1
                """;

                String insertMemberCouponQuery = """
                    INSERT INTO member_coupon (
                        customer_id, coupon_id, member_coupon_created_at, member_coupon_expired_at, member_coupon_used_at
                    )
                    VALUES (?, ?, NOW(), ?, NULL)
                """;

                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

                for (Member member : items) {
                    // Step 1: 유효한 쿠폰 정보 가져오기
                    List<Map<String, Object>> couponList = jdbcTemplate.queryForList(couponQuery);

                    if (couponList.isEmpty()) {
                        throw new NotFoundException("생일 쿠폰이 존재하지 않습니다. 쿠폰 정책 혹은 쿠폰 관리를 확인하여주세요");
                    }

                    Map<String, Object> couponData = jdbcTemplate.queryForMap(couponQuery);

                    Long couponId = (Long) couponData.get("coupon_id");
                    Integer effectivePeriod = (Integer) couponData.get("coupon_effective_period");
                    Integer effectiveUnit = (Integer) couponData.get("coupon_effective_period_unit");
                    Date couponExpiredAt = (Date) couponData.get("coupon_expired_at");

                    // Step 2: 회원 쿠폰 만료일 계산
                    LocalDate expiredAt;
                    if (effectivePeriod != null && effectiveUnit != null) {
                        // 유효기간 단위를 고려한 만료일 계산
                        expiredAt = calculateExpiredDate(effectivePeriod, effectiveUnit);
                    } else if (couponExpiredAt != null) {
                        // 쿠폰 테이블의 만료일 사용
                        expiredAt = couponExpiredAt.toLocalDate();
                    } else {
                        throw new IllegalStateException("유효기간과 만료일이 없는 쿠폰입니다.");
                    }

                    // Step 3: member_coupon 테이블에 삽입
                    String checkExistQuery = """
                        SELECT COUNT(1)
                        FROM member_coupon
                        WHERE customer_id = ?
                          AND coupon_id = ?
                          AND YEAR(member_coupon_created_at) = YEAR(CURRENT_DATE())
                    """;

                    Integer count = jdbcTemplate.queryForObject(checkExistQuery, Integer.class, member.getCustomerId(), couponId);
                    count = count == null ? 0 : count;
                    if (count == 0){
                        jdbcTemplate.update(insertMemberCouponQuery, member.getCustomerId(), couponId, expiredAt);
                    }

                }
            }

            // 유효기간 단위를 고려한 만료일 계산
            private LocalDate calculateExpiredDate(int period, int unit) {
                LocalDate now = LocalDate.now();
                return switch (unit) {
                    case 0 -> // 일 단위
                            now.plusDays(period);
                    case 1 -> // 월 단위
                            now.plusMonths(period);
                    case 2 -> // 년 단위
                            now.plusYears(period);
                    default -> throw new IllegalArgumentException("알 수 없는 유효기간 단위: " + unit);
                };
            }
        };
    }



}
