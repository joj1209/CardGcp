# SqlReader 클래스 비교 분석

## 개요
프로젝트에 두 개의 `SqlReader` 클래스가 존재하며, 각각 다른 패키지에서 다른 목적으로 사용됩니다.

## 클래스 위치

### 1. service.queryParser.reader.SqlReader
- **경로**: `src/main/java/service/queryParser/reader/SqlReader.java`
- **용도**: 쿼리 파싱 서비스용 (file.job.AppJob, file.job.AppStepJob에서 사용)
- **특징**: 함수형 인터페이스 패턴 사용

### 2. service.fileUtil.reader.SqlReader
- **경로**: `src/main/java/service/fileUtil/reader/SqlReader.java`
- **용도**: 파일 유틸리티용 (service.fileUtil.job.UtilJob에서 사용)
- **특징**: 직접 처리 패턴 사용

---

## 주요 차이점 비교표

| 구분 | service.queryParser.SqlReader | service.fileUtil.SqlReader |
|------|------------------------------|---------------------------|
| **설계 패턴** | 콜백 패턴 (Handler 주입) | 직접 처리 패턴 |
| **인스턴스 변수** | charset (문자셋 선택 가능) | 없음 (상수만 사용) |
| **생성자** | 2개 (기본, 커스텀 charset) | 없음 (기본 생성자만) |
| **핵심 메서드** | run(Path, SqlFileHandler) | run(Path) |
| **문자셋 처리** | CharsetDecoder (에러 대체) | Files.readString (단순) |
| **파일/폴더 구분** | 폴더만 처리 | 파일/폴더 자동 감지 |
| **확장성** | 높음 (Handler 교체 가능) | 낮음 (고정 로직) |
| **재사용성** | 높음 (다양한 처리 주입) | 낮음 (출력만 고정) |
| **함수형 인터페이스** | SqlFileHandler 사용 | 없음 |
| **예외 처리** | IOException을 RuntimeException으로 변환 | IOException 직접 throw |

---

## 1. service.queryParser.SqlReader (Handler 패턴)

### 핵심 특징

#### 1.1 함수형 인터페이스: SqlFileHandler
```java
@FunctionalInterface
public interface SqlFileHandler {
    void handle(Path path, String sql) throws IOException;
}
```

#### 1.2 사용 방법
```java
SqlReader reader = new SqlReader(Charset.forName("EUC-KR"));
reader.run(inputDir, (path, sql) -> {
    // 파일 처리 로직을 자유롭게 구현
    TablesInfo info = processor.parse(sql);
    writer.write(path, info);
});
```

#### 1.3 장점
- **유연성**: 파일 처리 로직을 외부에서 주입
- **재사용성**: 동일한 Reader로 다양한 작업 수행
- **단일 책임 원칙**: Reader는 읽기만, 처리는 Handler가 담당
- **문자셋 안정성**: CharsetDecoder로 깨진 문자 대체

#### 1.4 단점
- **복잡성**: 초보자에게 콜백 패턴이 어려움
- **디버깅**: 핸들러 내부 오류 추적이 어려움

---

## 2. service.fileUtil.SqlReader (직접 처리 패턴)

### 핵심 특징

#### 2.1 파일/폴더 자동 감지
```java
public void run(Path inputPath) throws IOException {
    if (Files.isDirectory(inputPath)) {
        processDirectory(inputPath);
    } else if (Files.isRegularFile(inputPath)) {
        processFile(inputPath);
    }
}
```

#### 2.2 사용 방법
```java
SqlReader reader = new SqlReader();
reader.run(Paths.get("D:\\sample.sql"));  // 파일
reader.run(Paths.get("D:\\folder"));       // 폴더
```

#### 2.3 장점
- **간결성**: 바로 실행 가능, 추가 코드 불필요
- **명확성**: 처리 로직이 클래스 내부에 명시
- **유연성**: 파일과 폴더 모두 처리 가능

#### 2.4 단점
- **확장성 제한**: 다른 처리를 하려면 클래스 수정 필요
- **고정 출력**: System.out만 사용, 커스터마이징 불가
- **재사용 제한**: 다른 작업에는 사용 불가

---

## SqlFileHandler의 핵심 가치

### 1. **전략 패턴 (Strategy Pattern)**
Handler를 교체함으로써 동일한 Reader로 다양한 작업 수행:

```java
// 사용 예시 1: 테이블 추출
reader.run(inputDir, (path, sql) -> {
    TablesInfo info = tableParser.parse(sql);
    csvWriter.write(info);
});

// 사용 예시 2: SQL 검증
reader.run(inputDir, (path, sql) -> {
    validator.validate(sql);
    reporter.report(path, errors);
});

// 사용 예시 3: 통계 수집
reader.run(inputDir, (path, sql) -> {
    int lines = sql.split("\n").length;
    statistics.add(path, lines);
});
```

### 2. **관심사의 분리 (Separation of Concerns)**
- **SqlReader**: 파일 읽기 + 인코딩 처리
- **SqlFileHandler**: 비즈니스 로직 (파싱, 변환, 저장 등)

### 3. **테스트 용이성**
```java
// Mock Handler로 테스트 가능
reader.run(testDir, (path, sql) -> {
    processedFiles.add(path);
    contents.put(path, sql);
});
```

---

## 실제 사용 사례

### AppJob에서의 사용 (queryParser.SqlReader)
```java
public class AppJob {
    private final SqlReader reader;
    private final FileParserProcessor processor;
    private final TextWriter writer;
    
    public void execute() {
        reader.run(inputDir, (path, sql) -> {
            TablesInfo info = processor.parse(sql);
            writer.writeTables(path, info);
        });
    }
}
```

### UtilJob에서의 사용 (fileUtil.SqlReader)
```java
public class UtilJob {
    public static void main(String[] args) throws IOException {
        SqlReader reader = new SqlReader();
        reader.run(Paths.get(args[0]));
        // 파일 정보가 콘솔에 출력됨
    }
}
```

---

## 언제 어떤 것을 사용할까?

### queryParser.SqlReader를 사용해야 할 때:
- ✅ 복잡한 비즈니스 로직이 필요한 경우
- ✅ 여러 종류의 파일 처리 작업이 있는 경우
- ✅ 다양한 문자셋을 지원해야 하는 경우
- ✅ 테스트 코드 작성이 중요한 경우
- ✅ **AppJob, AppStepJob 같은 파이프라인 구조**

### fileUtil.SqlReader를 사용해야 할 때:
- ✅ 간단한 파일 확인/출력만 필요한 경우
- ✅ 빠른 프로토타이핑이 필요한 경우
- ✅ 파일/폴더를 구분 없이 처리하고 싶은 경우
- ✅ **UtilJob 같은 단순 유틸리티 도구**

---

## 개선 제안

### 1. fileUtil.SqlReader에 Handler 패턴 추가
현재 고정된 출력 대신 Handler를 선택적으로 받을 수 있도록:

```java
// 기본 동작 (하위 호환성)
public void run(Path inputPath) throws IOException {
    run(inputPath, this::defaultProcess);
}

// Handler 주입 가능
public void run(Path inputPath, SqlFileHandler handler) throws IOException {
    // ...
}

private void defaultProcess(Path file, String content) {
    System.out.println("File: " + file);
    System.out.println("Content length: " + content.length());
}
```

### 2. queryParser.SqlReader에 파일/폴더 자동 감지 추가
```java
public void run(Path inputPath, SqlFileHandler handler) {
    if (Files.isDirectory(inputPath)) {
        processDirectory(inputPath, handler);
    } else if (Files.isRegularFile(inputPath)) {
        processFile(inputPath, handler);
    }
}
```

---

## 결론

두 `SqlReader` 클래스는 **서로 다른 철학**을 가지고 있습니다:

1. **service.queryParser.SqlReader**
   - 객체지향적, 함수형 프로그래밍
   - 확장성과 재사용성 중시
   - 복잡한 파이프라인에 적합

2. **service.fileUtil.SqlReader**
   - 절차적, 직관적
   - 간결성과 즉시 사용 가능성 중시
   - 단순 유틸리티에 적합

**SqlFileHandler의 가치**는 **"읽기"와 "처리"의 분리**에 있으며, 이는 Spring Batch의 `ItemReader`-`ItemProcessor` 분리와 동일한 철학입니다.

현재 프로젝트에서는 두 가지 접근 방식이 각각의 용도에 맞게 잘 사용되고 있습니다.

