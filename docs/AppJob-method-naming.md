# AppJob 메소드명 변경 (Spring Batch 스타일 적용)

## 변경 개요

AppJob 클래스의 메소드명을 Spring Batch의 명명 규칙에 맞춰 변경하였습니다. 이를 통해 Spring Batch와의 개념적 일관성을 높이고 코드의 의도를 더욱 명확하게 표현합니다.

## 변경 내역

| 이전 메소드명 | 새 메소드명 | 설명 | Spring Batch 유사 메소드 |
|-------------|-----------|------|----------------------|
| `buildFactory()` | `createJob()` | Job 인스턴스 생성 | `JobBuilder.build()` |
| `stepRead()` | `execute()` | Job 실행 | `JobLauncher.run()` |
| `handleFile()` | `processFile()` | 파일 처리 | `Step.execute()` |
| `stepParse()` | `process()` | 데이터 파싱 | `ItemProcessor.process()` |
| `stepWrite()` | `write()` | 결과 쓰기 | `ItemWriter.write()` |

## 변경 사유

### 1. createJob() (이전: buildFactory)

**변경 이유**:
- Spring Batch에서는 `JobBuilder`를 사용하여 Job을 생성합니다
- "create"는 객체 생성을 명확히 표현하는 표준 명명 규칙
- "Factory"는 디자인 패턴 이름으로, 메소드명에 포함시키는 것은 부적절

**Spring Batch 유사 코드**:
```java
@Bean
public Job importUserJob(JobRepository jobRepository) {
    return new JobBuilder("importUserJob", jobRepository)
        .start(step1())
        .build();
}
```

### 2. execute() (이전: stepRead)

**변경 이유**:
- "stepRead"는 Step의 일부 기능(읽기)만을 암시
- 실제로는 전체 Job을 실행하므로 "execute"가 더 적합
- Spring Batch의 `Step.execute()`, `Job.execute()` 메소드와 일관성 유지

**Spring Batch 유사 코드**:
```java
// JobLauncher로 Job 실행
JobExecution execution = jobLauncher.run(job, jobParameters);

// Step도 execute() 메소드를 가짐
public void execute(StepExecution stepExecution);
```

### 3. processFile() (이전: handleFile)

**변경 이유**:
- "handle"은 너무 일반적인 용어
- "process"는 데이터 처리를 명확히 표현
- Spring Batch의 Step 실행 개념과 유사

**Spring Batch 유사 코드**:
```java
@Bean
public Step step1() {
    return stepBuilderFactory.get("step1")
        .<InputType, OutputType>chunk(100)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .build();
}
```

### 4. process() (이전: stepParse)

**변경 이유**:
- Spring Batch의 `ItemProcessor.process()` 메소드와 정확히 일치
- "Parse"는 구체적인 구현 내용을 노출 (파싱은 내부 동작)
- "Process"는 더 추상적이고 확장 가능한 개념

**Spring Batch 유사 코드**:
```java
public interface ItemProcessor<I, O> {
    O process(I item) throws Exception;
}

// 구현 예시
public class PersonProcessor implements ItemProcessor<Person, Person> {
    @Override
    public Person process(Person person) throws Exception {
        // 데이터 처리 로직
        return transformedPerson;
    }
}
```

### 5. write() (이전: stepWrite)

**변경 이유**:
- Spring Batch의 `ItemWriter.write()` 메소드와 정확히 일치
- "step" 접두사는 불필요 (이미 클래스 컨텍스트에서 명확)
- 간결하고 명확한 의도 표현

**Spring Batch 유사 코드**:
```java
public interface ItemWriter<T> {
    void write(List<? extends T> items) throws Exception;
}

// 구현 예시
public class PersonWriter implements ItemWriter<Person> {
    @Override
    public void write(List<? extends Person> items) throws Exception {
        // 데이터 쓰기 로직
    }
}
```

## 코드 변경 전후 비교

### 변경 전

```java
public class AppJob {
    public static AppJob buildFactory() {
        // Job 생성
    }
    
    public void stepRead() {
        reader.run(inputDir, this::handleFile);
    }
    
    private void handleFile(Path file, String sql) {
        TablesInfo info = stepParse(sql);
        stepWrite(file, info);
    }
    
    private TablesInfo stepParse(String sql) {
        return processor.parse(sql);
    }
    
    private void stepWrite(Path file, TablesInfo info) {
        writer.writeTables(inputDir, file, info);
    }
    
    public static void main(String[] args) {
        AppJob job = buildFactory();
        job.stepRead();
    }
}
```

### 변경 후

```java
public class AppJob {
    public static AppJob createJob() {
        // Job 생성
    }
    
    public void execute() {
        reader.run(inputDir, this::processFile);
    }
    
    private void processFile(Path file, String sql) {
        TablesInfo info = process(sql);
        write(file, info);
    }
    
    private TablesInfo process(String sql) {
        return processor.parse(sql);
    }
    
    private void write(Path file, TablesInfo info) {
        writer.writeTables(inputDir, file, info);
    }
    
    public static void main(String[] args) {
        AppJob job = createJob();
        job.execute();
    }
}
```

## 사용 예시 변경

### 변경 전

```java
// 기본 설정으로 실행
AppJob job = AppJob.buildFactory();
job.stepRead();

// 커스텀 설정으로 실행
AppJob customJob = new AppJob(inputDir, reader, processor, writer);
customJob.stepRead();
```

### 변경 후

```java
// 기본 설정으로 실행
AppJob job = AppJob.createJob();
job.execute();

// 커스텀 설정으로 실행
AppJob customJob = new AppJob(inputDir, reader, processor, writer);
customJob.execute();
```

## Spring Batch와의 비교

### Spring Batch 표준 코드

```java
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
    
    @Bean
    public Job importUserJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("importUserJob", jobRepository)
            .start(step1)
            .build();
    }
    
    @Bean
    public Step step1(JobRepository jobRepository, 
                      PlatformTransactionManager transactionManager) {
        return new StepBuilder("step1", jobRepository)
            .<Person, Person>chunk(10, transactionManager)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .build();
    }
    
    @Bean
    public ItemReader<Person> reader() {
        return new FlatFileItemReaderBuilder<Person>()
            .name("personItemReader")
            .resource(new ClassPathResource("data.csv"))
            .delimited()
            .names(new String[]{"firstName", "lastName"})
            .fieldSetMapper(new BeanWrapperFieldSetMapper<>())
            .build();
    }
    
    @Bean
    public ItemProcessor<Person, Person> processor() {
        return person -> {
            // 처리 로직
            return transformedPerson;
        };
    }
    
    @Bean
    public ItemWriter<Person> writer() {
        return items -> {
            // 쓰기 로직
        };
    }
}

// Job 실행
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### AppJob 현재 코드 (변경 후)

```java
public class AppJob {
    
    public static AppJob createJob() {
        Path input = Paths.get("D:", "11. Project", "11. DB", "BigQuery");
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileParserProcessor processor = FileParserProcessor.withDefaults();
        TextWriter writer = new TextWriter(TextWriter.DEFAULT_OUTPUT_DIR, 
                                           Charset.forName("UTF-8"));
        return new AppJob(input, reader, processor, writer);
    }
    
    public void execute() {
        reader.run(inputDir, this::processFile);
    }
    
    private void processFile(Path file, String sql) {
        TablesInfo info = process(sql);
        write(file, info);
    }
    
    private TablesInfo process(String sql) {
        return processor.parse(sql);
    }
    
    private void write(Path file, TablesInfo info) {
        writer.writeTables(inputDir, file, info);
    }
    
    public static void main(String[] args) {
        AppJob job = createJob();
        job.execute();
    }
}
```

## 개념 매핑

| Spring Batch | AppJob | 설명 |
|-------------|--------|------|
| `Job` | `AppJob` | 전체 배치 작업 |
| `JobBuilder.build()` | `createJob()` | Job 인스턴스 생성 |
| `JobLauncher.run()` | `execute()` | Job 실행 |
| `Step.execute()` | `processFile()` | 단일 항목 처리 |
| `ItemReader.read()` | `SqlReader.run()` | 데이터 읽기 |
| `ItemProcessor.process()` | `process()` | 데이터 처리 |
| `ItemWriter.write()` | `write()` | 데이터 쓰기 |

## 장점

### 1. 명확한 의도 표현
- `createJob()`: Job을 생성한다는 의도가 명확
- `execute()`: Job을 실행한다는 의도가 명확
- `process()`: 데이터를 처리한다는 의도가 명확
- `write()`: 데이터를 쓴다는 의도가 명확

### 2. 표준 명명 규칙 준수
- Spring Batch의 표준 메소드명과 일치
- Java Bean 명명 규칙 준수
- Clean Code 원칙 준수 (의미 있는 이름)

### 3. 확장성 향상
- `process()`: 파싱 외에 다른 처리도 추가 가능
- 메소드명이 구체적인 구현에 의존하지 않음

### 4. 학습 용이성
- Spring Batch를 학습한 개발자가 쉽게 이해 가능
- 일관된 용어 사용으로 혼란 감소

## 주의사항

### 기존 코드와의 호환성
이번 변경으로 기존 메소드명을 사용하던 코드는 수정이 필요합니다:

```java
// 수정 필요
AppJob job = AppJob.buildFactory();  // ❌
job.stepRead();                       // ❌

// 변경 후
AppJob job = AppJob.createJob();      // ✅
job.execute();                        // ✅
```

### 문서 업데이트
다음 문서들이 함께 업데이트되었습니다:
- `docs/AppJob.md` - AppJob 클래스 문서
- `docs/AppJob-vs-SpringBatch.md` - Spring Batch 비교 문서

## 결론

이번 메소드명 변경은 다음과 같은 효과를 가져옵니다:

1. **Spring Batch와의 개념적 일관성 향상**
   - 표준 명명 규칙 적용
   - 업계 표준과의 정렬

2. **코드 가독성 향상**
   - 메소드의 역할이 명확히 드러남
   - 불필요한 접두사 제거

3. **유지보수성 향상**
   - 직관적인 메소드명으로 이해 용이
   - Spring Batch 경험이 있는 개발자의 빠른 적응

4. **확장성 향상**
   - 구체적인 구현에 의존하지 않는 추상적 이름
   - 미래 변경사항 수용 용이

비록 AppJob은 Spring Batch의 완전한 구현은 아니지만, 표준 명명 규칙을 따름으로써 코드의 품질과 전문성을 높였습니다.

