package com.nhnacademy.batch.scheduler;

import com.nhnacademy.batch.config.BirthdayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BirthdayScheduler {

    private final JobLauncher jobLauncher; // Job 실행기
    private final BirthdayConfig birthdayConfig;

    // 매일 오전 0시 1분에 실행
    @Scheduled(cron = "0 1 0 * * ?")
    public void runBirthdayJob() {
        try {
            // JobParameters 생성 (중복 실행 방지를 위해 시간 추가)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis()) // 고유 파라미터 추가
                    .toJobParameters();

            // Job 실행
            jobLauncher.run(birthdayConfig.birthdayJob(), jobParameters);
            log.info("Birthday Job 실행 성공");
        } catch (Exception e) {
            log.error("Birthday Job 실행 중 오류 발생", e);
            throw new IllegalStateException("생일 쿠폰 스캐쥴러 실패");

        }
    }
}
