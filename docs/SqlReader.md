# SqlReader 클래스 문서

## 개요

`SqlReader`는 지정한 문자셋으로 SQL 파일을 읽어들이는 유틸리티 클래스입니다. 다양한 인코딩을 다루면서 안전하게 파일 내용을 가져올 때 사용합니다.

이 클래스는 Spring Batch의 `ItemReader` 개념을 간소화한 형태로, SQL 파일을 읽는 Reader 역할을 수행합니다.

## 주요 특징

- **유연한 문자셋 지원**: UTF-8, EUC-KR 등 다양한 인코딩 지원
- **안전한 인코딩 처리**: 잘못된 문자 인코딩을 자동으로 대체
- **디렉토리 순회**: 지정된 디렉토리의 모든 `.sql` 파일을 재귀적으로 처리
- **함수형 인터페이스**: `SqlFileHandler`를 통한 유연한 콜백 처리

## 상수

### DEFAULT_INPUT_DIR
- **타입**: `Path` (static final)
- **값**: `D:\11. Project\11. DB\BigQuery`
- **설명**: SQL 파일을 읽을 기본 입력 디렉토리 경로입니다.

### UTF8
- **타입**: `Charset` (static final)
- **값**: `Charset.forName("UTF-8")`
- **설명**: UTF-8 문자셋 상수입니다.

### DEFAULT_CHARSET
- **타입**: `Charset` (static final)
- **값**: `UTF8`
- **설명**: 기본으로 사용할 문자셋입니다. 현재는 UTF-8로 설정되어 있습니다.

## 필드

### charset
- **타입**: `Charset` (private final)
- **설명**: SQL 파일을 읽을 때 사용할 문자셋입니다. 생성자를 통해 설정되며, 이후 변경할 수 없습니다.

## 생성자

### SqlReader()

기본 생성자입니다. 기본 문자셋(UTF-8)을 사용하여 인스턴스를 생성합니다.

```java
SqlReader reader = new SqlReader();
```

내부적으로 `SqlReader(DEFAULT_CHARSET)`를 호출합니다.

### SqlReader(Charset charset)

지정한 문자셋을 사용하는 SqlReader 인스턴스를 생성합니다.

**파라미터**:
- `charset` - SQL 파일을 읽을 때 사용할 문자셋 (null 불가)

**사용 예시**:
```java
// EUC-KR 인코딩으로 파일 읽기
SqlReader eucKrReader = new SqlReader(Charset.forName("EUC-KR"));

// UTF-8 인코딩으로 파일 읽기 (명시적)
SqlReader utf8Reader = new SqlReader(Charset.forName("UTF-8"));
```

## 메서드

### readFile(Path file)

지정된 경로의 파일을 읽어 문자열로 반환합니다.

**동작 방식**:
1. 파일의 모든 바이트를 읽어들입니다.
2. 지정된 문자셋으로 디코더를 생성합니다.
3. 잘못된 입력(malformed input)이나 매핑 불가능한 문자는 자동으로 대체 문자로 치환합니다.
4. 디코딩된 문자열을 반환합니다.

**파라미터**:
- `file` - 읽을 파일의 절대 경로

**반환값**: 파일 내용 문자열

**예외**:
- `IOException` - 파일 읽기 중 I/O 오류가 발생한 경우

**특징**:
- `CodingErrorAction.REPLACE`를 사용하여 인코딩 오류에 강건하게 대응
- 손상된 문자가 있어도 전체 읽기가 실패하지 않음

**사용 예시**:
```java
SqlReader reader = new SqlReader(Charset.forName("EUC-KR"));
Path sqlFile = Paths.get("D:", "sql", "sample.sql");
String content = reader.readFile(sqlFile);
System.out.println(content);
```

### readFile(String relativeFile)

기본 입력 디렉토리에 상대 경로로 지정된 파일을 읽어들입니다.

**동작 방식**:
1. `DEFAULT_INPUT_DIR`과 `relativeFile`을 결합하여 절대 경로를 생성합니다.
2. `readFile(Path)` 메서드를 호출하여 파일 내용을 읽습니다.

**파라미터**:
- `relativeFile` - 기본 디렉토리 기준 상대 경로 (예: `"subfolder/sample.sql"`)

**반환값**: 파일 내용 문자열

**예외**:
- `IOException` - 파일 읽기 중 문제가 발생한 경우

**사용 예시**:
```java
SqlReader reader = new SqlReader();
// D:\11. Project\11. DB\BigQuery\test\sample.sql 파일을 읽음
String content = reader.readFile("test/sample.sql");
```

### run(Path inputDir, SqlFileHandler handler)

입력 디렉토리를 순회하며 모든 `.sql` 파일을 읽고 핸들러에 전달합니다.

**동작 방식**:
1. `Files.walk()`를 사용하여 `inputDir` 아래의 모든 파일과 디렉토리를 재귀적으로 탐색합니다.
2. 일반 파일만 필터링합니다 (`Files::isRegularFile`).
3. 파일명이 `.sql`로 끝나는 파일만 선택합니다.
4. 각 SQL 파일에 대해 `handle()` 메서드를 호출합니다.
5. `handle()` 메서드는 파일을 읽고 `SqlFileHandler`의 `handle()` 메서드를 호출합니다.

**파라미터**:
- `inputDir` - SQL 파일이 위치한 입력 디렉토리의 절대 경로
- `handler` - 각 SQL 파일을 처리할 핸들러 (함수형 인터페이스)

**예외**:
- `RuntimeException` - 디렉토리 순회 중 `IOException`이 발생한 경우 래핑하여 던집니다.

**사용 예시**:
```java
SqlReader reader = new SqlReader(Charset.forName("EUC-KR"));
Path inputDir = Paths.get("D:", "sql", "batch");

// 람다 표현식 사용
reader.run(inputDir, (path, sql) -> {
    System.out.println("Processing: " + path.getFileName());
    System.out.println("Content length: " + sql.length());
});

// 메서드 참조 사용
reader.run(inputDir, this::processSqlFile);
```

**처리 흐름**:
```
run()
  → Files.walk(inputDir)
  → filter(isRegularFile)
  → filter(*.sql)
  → forEach(path -> handle(path, handler))
      → readFile(path)
      → handler.handle(path, sql)
```

### handle(Path path, SqlFileHandler handler)

단일 SQL 파일을 읽고 핸들러에 전달하는 내부 메서드입니다.

**동작 방식**:
1. `readFile(path)`를 호출하여 SQL 파일 내용을 읽습니다.
2. `handler.handle(path, sql)`을 호출하여 파일 경로와 내용을 전달합니다.
3. 파일 읽기 중 `IOException`이 발생하면 에러 메시지를 출력하고 계속 진행합니다.

**파라미터**:
- `path` - 읽을 SQL 파일의 경로
- `handler` - 파일 처리 핸들러

**예외 처리**:
- 파일 읽기 실패 시 해당 파일을 건너뛰고 다음 파일 처리를 계속합니다.
- 에러 메시지는 표준 에러 스트림(`System.err`)에 출력됩니다.

**에러 메시지 형식**:
```
파일 읽기 실패: D:\sql\sample.sql - (오류 메시지)
```

## 내부 인터페이스

### SqlFileHandler

SQL 파일을 처리하기 위한 함수형 인터페이스입니다.

```java
@FunctionalInterface
public interface SqlFileHandler {
    void handle(Path path, String sql) throws IOException;
}
```

**메서드**:
- `handle(Path path, String sql)` - SQL 파일 경로와 내용을 받아 처리합니다.

**파라미터**:
- `path` - SQL 파일의 절대 경로
- `sql` - 파일의 전체 내용 (문자열)

**예외**:
- `IOException` - 파일 처리 중 I/O 오류가 발생할 수 있습니다.

**특징**:
- `@FunctionalInterface` 어노테이션으로 람다 표현식 사용 가능
- Spring Batch의 콜백 패턴과 유사한 구조

**사용 예시**:
```java
// 람다 표현식
SqlFileHandler handler1 = (path, sql) -> {
    System.out.println("File: " + path);
    System.out.println("Content: " + sql);
};

// 메서드 참조
SqlFileHandler handler2 = this::processFile;

// 익명 클래스
SqlFileHandler handler3 = new SqlFileHandler() {
    @Override
    public void handle(Path path, String sql) throws IOException {
        // 처리 로직
    }
};
```

## 사용 예시

### 기본 사용법

```java
// UTF-8로 파일 읽기
SqlReader reader = new SqlReader();
String content = reader.readFile(Paths.get("D:", "sql", "sample.sql"));
System.out.println(content);
```

### 다른 인코딩 사용

```java
// EUC-KR로 파일 읽기
SqlReader eucKrReader = new SqlReader(Charset.forName("EUC-KR"));
String content = eucKrReader.readFile(Paths.get("D:", "sql", "legacy.sql"));
```

### 상대 경로로 읽기

```java
SqlReader reader = new SqlReader();
// D:\11. Project\11. DB\BigQuery\test.sql 읽기
String content = reader.readFile("test.sql");
```

### 디렉토리 전체 처리

```java
SqlReader reader = new SqlReader(Charset.forName("EUC-KR"));
Path inputDir = Paths.get("D:", "11. Project", "11. DB", "BigQuery");

reader.run(inputDir, (path, sql) -> {
    System.out.println("Processing: " + path.getFileName());
    
    // SQL 파싱 또는 다른 처리
    TablesInfo info = parser.parse(sql);
    
    // 결과 저장
    writer.write(path, info);
});
```

### AppJob에서의 사용

```java
public class AppJob {
    private final SqlReader reader;
    
    public void execute() {
        reader.run(inputDir, this::processFile);
    }
    
    private void processFile(Path file, String sql) {
        TablesInfo info = process(sql);
        write(file, info);
    }
}
```

## 인코딩 처리 전략

### CodingErrorAction.REPLACE

`SqlReader`는 잘못된 인코딩을 만났을 때 `REPLACE` 전략을 사용합니다:

```java
CharsetDecoder dec = charset.newDecoder()
    .onMalformedInput(CodingErrorAction.REPLACE)      // 잘못된 입력 대체
    .onUnmappableCharacter(CodingErrorAction.REPLACE); // 매핑 불가 문자 대체
```

**장점**:
- 파일에 일부 손상된 문자가 있어도 전체 읽기가 실패하지 않음
- 대체 문자(`�`)로 치환되어 계속 처리 가능

**대안**:
- `CodingErrorAction.REPORT`: 예외를 발생시킴 (엄격한 검증)
- `CodingErrorAction.IGNORE`: 잘못된 문자를 무시함

## 에러 처리

### 파일 읽기 실패

```java
private void handle(Path path, SqlFileHandler handler) {
    try {
        handler.handle(path, readFile(path));
    } catch (IOException ex) {
        System.err.println("파일 읽기 실패: " + path + " - " + ex.getMessage());
    }
}
```

**특징**:
- 개별 파일 읽기 실패 시 해당 파일만 스킵
- 다른 파일의 처리는 계속 진행
- 에러 로그는 표준 에러 스트림에 출력

### 디렉토리 순회 실패

```java
public void run(Path inputDir, SqlFileHandler handler) {
    try (Stream<Path> paths = Files.walk(inputDir)) {
        // ...
    } catch (IOException e) {
        throw new RuntimeException("입력 디렉터리 순회 중 오류", e);
    }
}
```

**특징**:
- 디렉토리 순회 자체가 실패하면 `RuntimeException`으로 래핑
- 전체 처리가 중단됨

## Spring Batch와의 비교

| 측면 | Spring Batch ItemReader | SqlReader |
|------|------------------------|-----------|
| **인터페이스** | `ItemReader<T>` | 커스텀 클래스 |
| **읽기 방식** | `read()` - 한 번에 1개 | `run()` - 전체 순회 |
| **콜백** | 없음 (반복문에서 호출) | `SqlFileHandler` |
| **상태 관리** | Stateful (ExecutionContext) | Stateless |
| **재시작 지원** | 지원 | 미지원 |
| **트랜잭션** | Step 트랜잭션과 통합 | 없음 |

### Spring Batch ItemReader 예시

```java
public class SqlFileItemReader implements ItemReader<String> {
    private Iterator<Path> fileIterator;
    
    @Override
    public String read() throws Exception {
        if (fileIterator.hasNext()) {
            Path file = fileIterator.next();
            return readFile(file);
        }
        return null; // 더 이상 읽을 항목 없음
    }
}
```

### SqlReader 사용 예시

```java
SqlReader reader = new SqlReader();
reader.run(inputDir, (path, sql) -> {
    // 각 파일 처리
});
```

**차이점**:
- **Spring Batch**: Pull 방식 (호출자가 `read()` 반복 호출)
- **SqlReader**: Push 방식 (모든 파일을 순회하며 핸들러 호출)

## 개선 제안

### 1. 진행 상황 로깅 추가

```java
public void run(Path inputDir, SqlFileHandler handler) {
    try (Stream<Path> paths = Files.walk(inputDir)) {
        List<Path> sqlFiles = paths
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().endsWith(".sql"))
            .collect(Collectors.toList());
        
        System.out.println("[Reader] Found " + sqlFiles.size() + " SQL files");
        
        for (int i = 0; i < sqlFiles.size(); i++) {
            Path path = sqlFiles.get(i);
            System.out.println("[Reader] Processing (" + (i+1) + "/" + sqlFiles.size() + "): " + path.getFileName());
            handle(path, handler);
        }
    } catch (IOException e) {
        throw new RuntimeException("입력 디렉터리 순회 중 오류", e);
    }
}
```

### 2. 파일 필터링 유연화

```java
public void run(Path inputDir, Predicate<Path> fileFilter, SqlFileHandler handler) {
    try (Stream<Path> paths = Files.walk(inputDir)) {
        paths.filter(Files::isRegularFile)
             .filter(fileFilter)
             .forEach(path -> handle(path, handler));
    } catch (IOException e) {
        throw new RuntimeException("입력 디렉터리 순회 중 오류", e);
    }
}

// 사용 예시
reader.run(inputDir, p -> p.toString().endsWith(".sql"), handler);
```

### 3. 병렬 처리 지원

```java
public void runParallel(Path inputDir, SqlFileHandler handler) {
    try (Stream<Path> paths = Files.walk(inputDir)) {
        paths.filter(Files::isRegularFile)
             .filter(p -> p.getFileName().toString().endsWith(".sql"))
             .parallel() // 병렬 처리
             .forEach(path -> handle(path, handler));
    } catch (IOException e) {
        throw new RuntimeException("입력 디렉터리 순회 중 오류", e);
    }
}
```

## 관련 클래스

- `FileParserProcessor` - SQL 파싱을 담당하는 Processor
- `TextWriter` - 결과를 파일로 쓰는 Writer
- `AppJob` - Reader, Processor, Writer를 연결하는 Job
- `TablesInfo` - 테이블 정보를 저장하는 VO

## 설계 철학

1. **단일 책임 원칙**: SQL 파일 읽기만 담당
2. **유연성**: 다양한 문자셋 지원
3. **견고성**: 인코딩 오류에 강건하게 대응
4. **확장성**: 함수형 인터페이스로 유연한 처리
5. **간결성**: Spring Framework 의존성 없이 순수 Java로 구현

