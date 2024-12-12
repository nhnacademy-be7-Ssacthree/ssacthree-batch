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
    private final DataSource dataSource;

    @Bean
    public Job birthdayJob() {
        return new JobBuilder("birthdayJob", jobRepository)
            .start(birthdayStep())
            .build();
    }

    @Bean
    public Step birthdayStep() {
        return new StepBuilder("BirthdayStep", jobRepository)
            .<Member, Member>chunk(10, transactionManager)
            .reader(birthdayReader())
            .writer(birthdayWriter())
            .build();
    }

    @Bean
    public JdbcPagingItemReader<Member> birthdayReader() {
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("customer_id", Order.ASCENDING);

        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT customer_id, member_birthdate");
        queryProvider.setFromClause("FROM member m");
        queryProvider.setWhereClause("""
        WHERE EXTRACT(MONTH FROM m.member_birthdate) = EXTRACT(MONTH FROM CURRENT_DATE())
        AND EXTRACT(DAY FROM m.member_birthdate) = EXTRACT(DAY FROM CURRENT_DATE())
        AND m.member_status = 'ACTIVE'
        """);
        queryProvider.setSortKeys(sortKeys);

        return new JdbcPagingItemReaderBuilder<Member>()
            .name("birthdayReader")
            .dataSource(dataSource)
            .pageSize(50)
            .queryProvider(queryProvider)
            .rowMapper(new MemberCustomRowMapper())
            .build();
    }

    @Bean
    @SuppressWarnings("squid:S1854")
    public ItemWriter<Member> birthdayWriter() {
        return new BirthdayItemWriter(dataSource);
    }

    private static class BirthdayItemWriter implements ItemWriter<Member> {
        private final JdbcTemplate jdbcTemplate;

        BirthdayItemWriter(DataSource dataSource) {
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }

        @Override
        public void write(Chunk<? extends Member> items) {
            for (Member member : items) {
                Map<String, Object> couponData = fetchValidCoupon();
                LocalDate expiredAt = calculateCouponExpiration(couponData);

                if (!hasCouponForThisYear(member.getCustomerId(), (Long) couponData.get("coupon_id"))) {
                    insertMemberCoupon(member.getCustomerId(), (Long) couponData.get("coupon_id"), expiredAt);
                }
            }
        }

        private Map<String, Object> fetchValidCoupon() {
            String couponQuery = """
                SELECT c.coupon_id, c.coupon_effective_period, c.coupon_effective_period_unit, c.coupon_expired_at
                FROM coupon c
                INNER JOIN coupon_rule cr ON c.coupon_rule_id = cr.coupon_rule_id
                WHERE cr.coupon_rule_name LIKE '%생일%'
                AND cr.coupon_is_used
                AND (c.coupon_expired_at > CURRENT_DATE() OR c.coupon_expired_at IS NULL)
                LIMIT 1
            """;

            List<Map<String, Object>> couponList = jdbcTemplate.queryForList(couponQuery);
            if (couponList.isEmpty()) {
                throw new NotFoundException("생일 쿠폰이 존재하지 않습니다. 쿠폰 정책 혹은 쿠폰 관리를 확인하여주세요");
            }
            return couponList.getFirst();
        }

        private LocalDate calculateCouponExpiration(Map<String, Object> couponData) {
            Integer effectivePeriod = (Integer) couponData.get("coupon_effective_period");
            Integer effectiveUnit = (Integer) couponData.get("coupon_effective_period_unit");
            Date couponExpiredAt = (Date) couponData.get("coupon_expired_at");

            if (effectivePeriod != null && effectiveUnit != null) {
                return calculateExpiredDate(effectivePeriod, effectiveUnit);
            } else if (couponExpiredAt != null) {
                return couponExpiredAt.toLocalDate();
            } else {
                throw new IllegalStateException("유효기간과 만료일이 없는 쿠폰입니다.");
            }
        }

        private boolean hasCouponForThisYear(Long customerId, Long couponId) {
            String checkExistQuery = """
                SELECT COUNT(1)
                FROM member_coupon
                WHERE customer_id = ?
                  AND coupon_id = ?
                  AND YEAR(member_coupon_created_at) = YEAR(CURRENT_DATE())
            """;
            Integer count = jdbcTemplate.queryForObject(checkExistQuery, Integer.class, customerId, couponId);
            return count != null && count > 0;
        }

        private void insertMemberCoupon(Long customerId, Long couponId, LocalDate expiredAt) {
            String insertMemberCouponQuery = """
                INSERT INTO member_coupon (
                    customer_id, coupon_id, member_coupon_created_at, member_coupon_expired_at, member_coupon_used_at
                )
                VALUES (?, ?, NOW(), ?, NULL)
            """;
            jdbcTemplate.update(insertMemberCouponQuery, customerId, couponId, expiredAt);
        }

        private LocalDate calculateExpiredDate(int period, int unit) {
            LocalDate now = LocalDate.now();
            return switch (unit) {
                case 0 -> now.plusDays(period);
                case 1 -> now.plusMonths(period);
                case 2 -> now.plusYears(period);
                default -> throw new IllegalArgumentException("알 수 없는 유효기간 단위: " + unit);
            };
        }
    }
}