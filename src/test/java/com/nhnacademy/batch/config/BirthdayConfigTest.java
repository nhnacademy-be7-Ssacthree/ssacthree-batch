package com.nhnacademy.batch.config;

import com.nhnacademy.batch.domain.Member;
import com.nhnacademy.batch.domain.mapper.MemberCustomRowMapper;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.item.database.Order;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@Import(BirthdayConfig.class)
class BirthdayConfigTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job birthdayJob;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private BirthdayConfig birthdayConfig;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testBirthdayJobExecution() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncher.run(birthdayJob, jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Writer가 정상적으로 작동했는지 확인
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member_coupon WHERE customer_id = 1", Integer.class);
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void testBirthdayStepExecution() throws Exception {
        // Given
        JobExecution jobExecution = jobRepository.createJobExecution("testJob", new JobParameters());
        StepExecution stepExecution = new StepExecution("BirthdayStep", jobExecution);
        jobRepository.add(stepExecution);

        // When
        Step step = birthdayConfig.birthdayStep();
        step.execute(stepExecution);

        // Then
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }


    @Test
    void testBirthdayReader() throws Exception {
        // Given
        JdbcPagingItemReader<Member> reader = new JdbcPagingItemReader<>();

        // 필수 설정
        reader.setDataSource(dataSource);
        reader.setRowMapper(new MemberCustomRowMapper());

        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT customer_id");
        queryProvider.setFromClause("FROM member m");
        queryProvider.setWhereClause("WHERE EXTRACT(MONTH FROM m.member_birthdate) = EXTRACT(MONTH FROM CURRENT_DATE())\n" +
                "AND EXTRACT(DAY FROM m.member_birthdate) = EXTRACT(DAY FROM CURRENT_DATE())" +
                "AND m.member_status = 'ACTIVE'");
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("customer_id", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);

        reader.setQueryProvider(queryProvider);
        reader.setPageSize(10);

        // Reader 초기화
        reader.afterPropertiesSet();

        org.springframework.batch.item.ExecutionContext executionContext = new org.springframework.batch.item.ExecutionContext();

        // When
        reader.open(executionContext);
        Member member = reader.read();

        // Then
        assertThat(member).isNotNull();
        assertThat(member.getCustomerId()).isEqualTo(1L);
    }





}
