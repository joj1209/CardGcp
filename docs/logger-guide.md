# 로그 모듈 사용 가이드

## 📋 개요

**SimpleAppLogger**는 외부 라이브러리 없이 순수 Java만으로 작동하는 로깅 유틸리티입니다.

### 특징
- ✅ **외부 라이브러리 불필요** - JDK만 있으면 작동
- ✅ **Maven/Gradle 불필요** - javac로 바로 컴파일 가능
- ✅ **콘솔 + 파일 동시 출력** - 실시간 모니터링 및 로그 파일 보관
- ✅ **일별 로그 파일 자동 생성** - 날짜별로 로그 파일 자동 분리
- ✅ **에러 로그 별도 저장** - 에러는 별도 파일에 기록
- ✅ **스레드 안전** - ReentrantLock으로 멀티스레드 환경 지원
- ✅ **한글 지원** - UTF-8 인코딩으로 한글 완벽 지원

---

## 📁 파일 위치

```
src/main/java/common/log/SimpleAppLogger.java
```

---

## 🚀 기본 사용법

### 1. 로거 인스턴스 생성

```java
import common.log.SimpleAppLogger;

public class MyClass {
    private static final SimpleAppLogger log = SimpleAppLogger.getLogger(MyClass.class);
    
    public static void main(String[] args) {
        log.info("프로그램 시작");
    }
}
```

### 2. 로그 레벨별 사용

```java
// DEBUG - 디버그 정보
log.debug("디버그 메시지");
log.debug("변수 값: %s", variableName);

// INFO - 일반 정보
log.info("프로그램이 정상적으로 실행되었습니다");
log.info("처리 건수: %d", count);

// WARN - 경고
log.warn("경고 메시지");
log.warn("파일을 찾을 수 없습니다: %s", fileName);

// ERROR - 에러
log.error("에러 발생");
log.error("파일 처리 중 에러: %s", fileName);
log.error("예외 발생", exception);
```

---

## 📝 주요 메소드

### 프로그램 실행 관련

```java
// 프로그램 시작 로그
log.start("프로그램명");

// 프로그램 종료 로그
log.end("프로그램명");

// 프로그램 종료 (처리 건수 포함)
log.end("프로그램명", processedCount);
```

**출력 예:**
```
================================================================================
프로그램 시작: 프로그램명
시작 시간: 2025-11-19 14:30:15.123
================================================================================
```

### 파일 처리 관련

```java
// 파일 처리 시작
log.fileStart("sample.sql");

// 파일 처리 완료
log.fileEnd("sample.sql", 100);  // 100: 처리한 라인 수

// 파일 처리 에러
log.fileError("sample.sql", exception);
```

### SQL 처리 관련

```java
// SQL 파일 스캔 시작
log.sqlScanStart("D:\\11. Project\\11. DB");

// SQL 파일 스캔 완료
log.sqlScanEnd(50);  // 50: 스캔한 파일 수

// 테이블 추출 결과
log.tableExtracted("sample.sql", 5, 3);  // Source: 5개, Target: 3개
```

### 진행률 표시

```java
// 진행률 표시
log.progress(30, 100);  // 30/100 (30%)

// 단계별 진행
log.step(1, "파일 읽기");
log.step(2, "데이터 변환");
log.step(3, "파일 저장");
```

### 유틸리티

```java
// 구분선 출력
log.separator();

// 구분선 + 제목
log.separator("데이터 처리 시작");
```

---

## 💡 실제 사용 예제

### 예제 1: ConvertStep1 (파일 변환)

```java
package com.cardgcp;

import common.log.SimpleAppLogger;
import java.nio.file.*;

public class ConvertStep1 {
    
    private static final SimpleAppLogger log = SimpleAppLogger.getLogger(ConvertStep1.class);
    
    public static void main(String[] args) throws Exception {
        log.start("SQL 파일 변환");
        
        try {
            log.step(1, "입력 폴더 확인");
            // ... 로직
            
            log.step(2, "파일 변환 시작");
            int fileCount = processFiles();
            
            log.end("SQL 파일 변환", fileCount);
            
        } catch (Exception e) {
            log.error("실행 중 오류 발생", e);
        }
    }
    
    private static void processFile(Path file, int[] count) {
        try {
            log.fileStart(file.getFileName().toString());
            
            // 파일 처리 로직
            
            count[0]++;
            log.fileEnd(file.getFileName().toString(), 1);
            
            if (count[0] % 10 == 0) {
                log.info("처리 중... (%d개 파일)", count[0]);
            }
        } catch (IOException e) {
            log.fileError(file.getFileName().toString(), e);
        }
    }
}
```

### 예제 2: SimpleSourceTarget (테이블 추출)

```java
package convert;

import com.log.AppLogger;

public class SimpleSourceTarget {
    
    private static final AppLogger log = AppLogger.getLogger(SimpleSourceTarget.class);
    
    public static void main(String[] args) throws Exception {
        log.start("Source/Target 테이블 추출");
        
        String directory = "D:\\11. Project\\11. DB";
        log.sqlScanStart(directory);
        
        int fileCount = 0;
        // 스캔 로직
        
        log.sqlScanEnd(fileCount);
        log.end("Source/Target 테이블 추출", fileCount);
    }
    
    private static void scanFile(Path sqlFile) throws IOException {
        log.fileStart(sqlFile.getFileName().toString());
        
        // 테이블 추출 로직
        int sourceCount = 5;
        int targetCount = 3;
        
        log.tableExtracted(sqlFile.getFileName().toString(), sourceCount, targetCount);
    }
}
```

### 예제 3: SqlFileScanner (디렉토리 스캔)

```java
package service.scan.processor;

import common.log.SimpleAppLogger;

public class SqlFileScanner {
    private static final SimpleAppLogger log = SimpleAppLogger.getLogger(SqlFileScanner.class);
    
    public int scanDirectory(Path root) throws IOException {
        log.sqlScanStart(root.toString());
        
        final int[] cnt = {0};
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith(".sql")) {
                    try {
                        processor.processFile(file);
                        cnt[0]++;
                        if (cnt[0] % 10 == 0) {
                            log.progress(cnt[0], -1);
                        }
                    } catch (Exception e) {
                        log.fileError(file.getFileName().toString(), e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        log.sqlScanEnd(cnt[0]);
        return cnt[0];
    }
}
```

---

## 📂 로그 파일 위치

로그 파일은 다음 위치에 자동으로 생성됩니다:

```
D:/11. Project/11. DB_OUT3/logs/
  ├── application-2025-11-19.log        (일반 로그)
  ├── application-2025-11-20.log        (다음 날 로그)
  ├── application-error-2025-11-19.log  (에러 로그)
  └── application-error-2025-11-20.log  (다음 날 에러)
```

### 로그 파일 특징
- **일별 자동 생성**: 날짜가 바뀌면 새 파일 생성
- **에러 로그 분리**: ERROR 레벨은 별도 파일에 저장
- **UTF-8 인코딩**: 한글 완벽 지원
- **자동 추가 모드**: 프로그램 재실행 시 기존 로그에 추가

---

## 🔧 로그 설정 변경

`SimpleAppLogger.java` 파일에서 설정을 변경할 수 있습니다:

```java
// 로그 파일 저장 위치 변경
private static final String LOG_DIR = "D:/로그경로/logs";

// 파일 로그 끄기
private static final boolean ENABLE_FILE_LOG = false;

// 콘솔 로그 끄기
private static final boolean ENABLE_CONSOLE_LOG = false;

// 로그 레벨 변경 (DEBUG, INFO, WARN, ERROR)
private static Level currentLevel = Level.DEBUG;
```

### 로그 레벨 동적 변경

```java
// 프로그램 실행 중 로그 레벨 변경
SimpleAppLogger.setLevel(SimpleAppLogger.Level.DEBUG);
```

---

## 📊 로그 출력 형식

### 콘솔 출력 예시

```
2025-11-19 14:30:15.123 [main] INFO  SimpleSourceTarget - ================================================================================
2025-11-19 14:30:15.124 [main] INFO  SimpleSourceTarget - 프로그램 시작: Source/Target 테이블 추출
2025-11-19 14:30:15.125 [main] INFO  SimpleSourceTarget - 시작 시간: 2025-11-19 14:30:15.125
2025-11-19 14:30:15.126 [main] INFO  SimpleSourceTarget - ================================================================================
2025-11-19 14:30:15.127 [main] INFO  SimpleSourceTarget - 입력 폴더: D:\11. Project\11. DB
2025-11-19 14:30:15.128 [main] INFO  SimpleSourceTarget - 출력 폴더: D:\11. Project\11. DB_OUT3
2025-11-19 14:30:15.129 [main] INFO  SimpleSourceTarget - SQL 파일 스캔 시작: D:\11. Project\11. DB
2025-11-19 14:30:15.200 [main] INFO  SimpleSourceTarget - 파일 처리 시작: sample.sql
2025-11-19 14:30:15.250 [main] INFO  SimpleSourceTarget - 테이블 추출 - 파일: sample.sql, Source: 5개, Target: 3개
2025-11-19 14:30:15.300 [main] INFO  SimpleSourceTarget - SQL 파일 스캔 완료: 10개 파일
2025-11-19 14:30:15.301 [main] INFO  SimpleSourceTarget - ================================================================================
2025-11-19 14:30:15.302 [main] INFO  SimpleSourceTarget - 프로그램 종료: Source/Target 테이블 추출
2025-11-19 14:30:15.303 [main] INFO  SimpleSourceTarget - 처리 건수: 10
2025-11-19 14:30:15.304 [main] INFO  SimpleSourceTarget - 종료 시간: 2025-11-19 14:30:15.304
2025-11-19 14:30:15.305 [main] INFO  SimpleSourceTarget - ================================================================================
```

### 파일 출력 예시

파일에도 동일한 형식으로 저장되며, 에러 발생 시 스택 트레이스도 함께 기록됩니다.

---

## ⚠️ 주의사항

1. **로그 디렉토리 권한**: 로그 파일 저장 위치에 쓰기 권한이 있어야 합니다.
2. **디스크 공간**: 로그 파일이 계속 쌓이므로 주기적으로 정리하세요.
3. **성능**: 파일 I/O가 발생하므로 과도한 로그는 성능에 영향을 줄 수 있습니다.
4. **스레드 안전**: 멀티스레드 환경에서도 안전하게 사용할 수 있습니다.

---

## 🎯 팁

### 1. 조건부 로그

```java
if (log.isDebugEnabled()) {  // 이 메소드는 구현되지 않음
    log.debug("상세 정보: %s", expensiveOperation());
}

// 또는 로그 레벨을 INFO로 설정하면 DEBUG는 자동으로 출력되지 않음
SimpleAppLogger.setLevel(SimpleAppLogger.Level.INFO);
```

### 2. 진행률 표시

```java
for (int i = 0; i < total; i++) {
    // 처리 로직
    if (i % 100 == 0) {
        log.progress(i, total);
    }
}
```

### 3. 에러 처리

```java
try {
    // 위험한 작업
} catch (Exception e) {
    log.error("작업 실패: %s", taskName);
    log.error("", e);  // 스택 트레이스 출력
}
```

---

## 📚 추가 정보

- 로그 파일은 프로그램 실행과 동시에 자동으로 생성됩니다.
- 로그 레벨은 DEBUG < INFO < WARN < ERROR 순서입니다.
- 현재 레벨보다 낮은 로그는 출력되지 않습니다.
- 기본 로그 레벨은 INFO입니다.

---

## 🔗 관련 파일

- `com/log/AppLogger.java` - 로그 모듈 소스
- `convert/SimpleSourceTarget.java` - 사용 예제 1
- `convert/ConvertStep1.java` - 사용 예제 2
- `convert/ConvertStep2.java` - 사용 예제 3
- `service/scan/processor/SqlFileScanner.java` - 사용 예제 4

---

**작성일**: 2025-11-19  
**버전**: 1.0

