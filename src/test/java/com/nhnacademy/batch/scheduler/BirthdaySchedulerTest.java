package com.nhnacademy.batch.scheduler;

import com.nhnacademy.batch.config.BirthdayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@Import({BirthdayConfig.class, BirthdayScheduler.class})
class BirthdaySchedulerTest {

    @Autowired
    private BirthdayConfig birthdayConfig;

    @MockBean
    private JobLauncher jobLauncherMock;

    @MockBean
    private Job birthdayJobMock;

    @Autowired
    private BirthdayScheduler birthdayScheduler;

    @Test
    void testRunBirthdayJob_Success() throws Exception {
        // Given
        JobExecution mockJobExecution = mock(JobExecution.class);
        // jobLauncher.run(...) 호출 시 mockJobExecution 리턴
        when(jobLauncherMock.run(eq(birthdayJobMock), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        // @Scheduled는 자동 실행되지 않으므로 명시적으로 runBirthdayJob() 호출
        birthdayScheduler.runBirthdayJob();

        // Then
        // jobLauncher.run(...) 메서드가 한 번 호출되었는지 검증
        verify(jobLauncherMock, times(1)).run(eq(birthdayJobMock), any(JobParameters.class));
    }

    @Test
    void testRunBirthdayJob_Exception() throws Exception {
        // Given
        when(jobLauncherMock.run(eq(birthdayJobMock), any(JobParameters.class)))
                .thenThrow(new IllegalStateException("생일 쿠폰 스케쥴러 실패"));

        // When & Then
        // 메서드 호출 변경: birthdayConfig.birthdayJob() → birthdayScheduler.runBirthdayJob()
        try {
            birthdayScheduler.runBirthdayJob();
            fail("예외가 발생해야 하지만 발생하지 않았습니다.");
        } catch (IllegalStateException e) {
            // 예외가 발생했으므로 jobLauncher.run()이 실제로 호출되었는지 검증
            verify(jobLauncherMock, times(1)).run(eq(birthdayJobMock), any(JobParameters.class));
        }
    }
}
