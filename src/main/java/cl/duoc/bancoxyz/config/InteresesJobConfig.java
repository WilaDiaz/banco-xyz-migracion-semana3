package cl.duoc.bancoxyz.config;

import org.springframework.beans.factory.annotation.Value;
import cl.duoc.bancoxyz.dto.InteresDTO;
import cl.duoc.bancoxyz.model.InteresCuenta;
import cl.duoc.bancoxyz.policy.CustomRetryPolicy;
import cl.duoc.bancoxyz.policy.CustomSkipPolicy;
import cl.duoc.bancoxyz.processor.InteresProcessor;
import cl.duoc.bancoxyz.repository.InteresCuentaRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;



@Configuration
public class InteresesJobConfig {

    @Bean
    public FlatFileItemReader<InteresDTO> rawInteresesReader() {
        return new FlatFileItemReaderBuilder<InteresDTO>()
                .name("rawInteresesReader")
                .resource(new ClassPathResource("data/intereses.csv"))
                .delimited()
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .linesToSkip(1)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(InteresDTO.class);
                }})
                .build();
    }

        @Value("${batch.chunk-size}")
    private int chunkSize;
   
    @Bean
    public SynchronizedItemStreamReader<InteresDTO> interesesReader() {
        SynchronizedItemStreamReader<InteresDTO> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(rawInteresesReader());
        return reader;
    }

    @Bean
    public InteresProcessor interesesProcessor() {
        return new InteresProcessor();
    }

    @Bean
    public RepositoryItemWriter<InteresCuenta> interesesWriter(InteresCuentaRepository repository) {
        RepositoryItemWriter<InteresCuenta> writer = new RepositoryItemWriter<>();
        writer.setRepository(repository);
        writer.setMethodName("save");
        return writer;
    }

    @Bean
    public Step interesesStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             InteresCuentaRepository repository,
                             @Qualifier("batchTaskExecutor") TaskExecutor taskExecutor) {
        return new StepBuilder("interesesStep", jobRepository)
                .<InteresDTO, InteresCuenta>chunk(chunkSize, transactionManager)
                .reader(interesesReader())
                .processor(interesesProcessor())
                .writer(interesesWriter(repository))
                .faultTolerant()
                .skipPolicy(new CustomSkipPolicy(skipLimit))
                .retryPolicy(new CustomRetryPolicy(retryLimit))
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public Job interesesJob(JobRepository jobRepository, 
                            Step interesesStep,
                            JobExecutionListener jobExecutionListener) {
        return new JobBuilder("interesesJob", jobRepository)
                .listener(jobExecutionListener)
                .start(interesesStep)
                .build();
    }
    
    @Value("${batch.skip-limit}")
private int skipLimit;

@Value("${batch.retry-limit}")
private int retryLimit;

}