package cl.duoc.bancoxyz.config;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@Configuration
public class BatchConfig {

    private static final Logger log = LoggerFactory.getLogger(BatchConfig.class);

        @Value("${batch.threads.core}")
    private int corePoolSize;

        @Value("${batch.threads.max}")
    private int maxPoolSize;

        @Value("${batch.queue-capacity}")
    private int queueCapacity;


    @Bean(name = "batchTaskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("banco-thread-");
        executor.initialize();
        return executor;
    }



    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("================================================================================");
                log.info(">>> INICIO JOB: {} | Identificador: {}", 
                        jobExecution.getJobInstance().getJobName(), 
                        jobExecution.getId());
                log.info("================================================================================");
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                log.info("================================================================================");
                log.info(">>> FIN JOB: {} | Estado: {} | Duracion: {} ms", 
                        jobExecution.getJobInstance().getJobName(), 
                        jobExecution.getStatus(),
                        (System.currentTimeMillis() - jobExecution.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()));
                log.info("================================================================================");
            }
        };
    }
}
