package cl.duoc.bancoxyz.config;

import org.springframework.beans.factory.annotation.Value;
import cl.duoc.bancoxyz.dto.CuentaAnualDTO;
import cl.duoc.bancoxyz.model.CuentaAnual;
import cl.duoc.bancoxyz.policy.CustomRetryPolicy;
import cl.duoc.bancoxyz.policy.CustomSkipPolicy;
import cl.duoc.bancoxyz.processor.CuentaAnualProcessor;
import cl.duoc.bancoxyz.repository.CuentaAnualRepository;
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
import cl.duoc.bancoxyz.model.EstadoCuentaResumen;
import cl.duoc.bancoxyz.repository.EstadoCuentaResumenRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Configuration
public class CuentasAnualesJobConfig {

    @Bean
    public FlatFileItemReader<CuentaAnualDTO> rawCuentasAnualesReader() {
        return new FlatFileItemReaderBuilder<CuentaAnualDTO>()
                .name("rawCuentasAnualesReader")
                .resource(new ClassPathResource("data/cuentas_anuales.csv"))
                .delimited()
                .names("cuentaId", "fecha", "tipoTransaccion", "monto", "descripcion")
                .linesToSkip(1)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(CuentaAnualDTO.class);
                }})
                .build();
    }

   
    @Bean
    public SynchronizedItemStreamReader<CuentaAnualDTO> cuentasAnualesReader() {
        SynchronizedItemStreamReader<CuentaAnualDTO> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(rawCuentasAnualesReader());
        return reader;
    }

    @Bean
    public CuentaAnualProcessor cuentasAnualesProcessor() {
        return new CuentaAnualProcessor();
    }

    @Bean
    public RepositoryItemWriter<CuentaAnual> cuentasAnualesWriter(CuentaAnualRepository repository) {
        RepositoryItemWriter<CuentaAnual> writer = new RepositoryItemWriter<>();
        writer.setRepository(repository);
        writer.setMethodName("save");
        return writer;
    }

    @Bean
    public Step cuentasAnualesStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  CuentaAnualRepository repository,
                                  @Qualifier("batchTaskExecutor") TaskExecutor taskExecutor) {
        return new StepBuilder("cuentasAnualesStep", jobRepository)
                .<CuentaAnualDTO, CuentaAnual>chunk(chunkSize, transactionManager)
                .reader(cuentasAnualesReader())
                .processor(cuentasAnualesProcessor())
                .writer(cuentasAnualesWriter(repository))
                .faultTolerant()
                .skipPolicy(new CustomSkipPolicy(skipLimit))
                .retryPolicy(new CustomRetryPolicy(retryLimit))
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
public Step limpiarEstadosAnualesStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        CuentaAnualRepository cuentaAnualRepository,
        EstadoCuentaResumenRepository resumenRepository) {

    return new StepBuilder("limpiarEstadosAnualesStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {

                // Eliminar resultados de ejecuciones anteriores
                resumenRepository.deleteAll();
                cuentaAnualRepository.deleteAll();

                System.out.println(
                        ">>> LIMPIEZA COMPLETADA | Datos anuales anteriores eliminados"
                );

                return null;

            }, transactionManager)
            .build();
}


    @Bean
public Step consolidarEstadosAnualesStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        CuentaAnualRepository cuentaAnualRepository,
        EstadoCuentaResumenRepository resumenRepository) {

    return new StepBuilder("consolidarEstadosAnualesStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {




                // Obtener todos los movimientos ya procesados por el Step anterior
                List<CuentaAnual> movimientos = cuentaAnualRepository.findAll();



                // Agrupar movimientos por cuenta y año
                Map<String, List<CuentaAnual>> movimientosAgrupados =
                        movimientos.stream()
                                .filter(movimiento -> movimiento.getFecha() != null)
                                .collect(Collectors.groupingBy(movimiento ->
                                        movimiento.getCuentaId()
                                                + "-"
                                                + movimiento.getFecha().getYear()
                                ));

                for (Map.Entry<String, List<CuentaAnual>> entry
                        : movimientosAgrupados.entrySet()) {

                    List<CuentaAnual> movimientosCuenta = entry.getValue();

                    CuentaAnual primerMovimiento = movimientosCuenta.get(0);

                    Long cuentaId = primerMovimiento.getCuentaId();
                    Integer anio = primerMovimiento.getFecha().getYear();

                    double totalIngresos = movimientosCuenta.stream()
                            .filter(movimiento ->
                                    movimiento.getMonto() != null
                                            && movimiento.getMonto() > 0)
                            .mapToDouble(CuentaAnual::getMonto)
                            .sum();

                    double totalEgresos = movimientosCuenta.stream()
                            .filter(movimiento ->
                                    movimiento.getMonto() != null
                                            && movimiento.getMonto() < 0)
                            .mapToDouble(movimiento ->
                                    Math.abs(movimiento.getMonto()))
                            .sum();

                    double saldoAnual = totalIngresos - totalEgresos;

                    long cantidadMovimientos = movimientosCuenta.size();

                    EstadoCuentaResumen resumen =
                            new EstadoCuentaResumen(
                                    cuentaId,
                                    anio,
                                    totalIngresos,
                                    totalEgresos,
                                    saldoAnual,
                                    cantidadMovimientos
                            );

                    resumenRepository.save(resumen);

                    System.out.println(
                            ">>> ESTADO ANUAL GENERADO | Cuenta: "
                                    + cuentaId
                                    + " | Año: "
                                    + anio
                                    + " | Ingresos: "
                                    + totalIngresos
                                    + " | Egresos: "
                                    + totalEgresos
                                    + " | Saldo: "
                                    + saldoAnual
                                    + " | Movimientos: "
                                    + cantidadMovimientos
                    );
                }

                System.out.println(
                        ">>> CONSOLIDACIÓN DE ESTADOS ANUALES COMPLETADA | "
                                + movimientosAgrupados.size()
                                + " estados generados"
                );

                return null;

            }, transactionManager)
            .build();
}


@Value("${batch.chunk-size}")
private int chunkSize;

@Value("${batch.skip-limit}")
private int skipLimit;

@Value("${batch.retry-limit}")
private int retryLimit;

@Bean
public Job cuentasAnualesJob(
        JobRepository jobRepository,
        Step limpiarEstadosAnualesStep,
        Step cuentasAnualesStep,
        Step consolidarEstadosAnualesStep,
        JobExecutionListener jobExecutionListener) {

    return new JobBuilder("cuentasAnualesJob", jobRepository)
            .listener(jobExecutionListener)
            .start(limpiarEstadosAnualesStep)
            .next(cuentasAnualesStep)
            .next(consolidarEstadosAnualesStep)
            .build();
}

}