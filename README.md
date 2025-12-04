# CardGcp

SQL 파일 변환 및 분석 도구

## 📋 주요 기능

### 1. SQL 파일 변환 (convert 패키지)
- **ConvertStep1**: 주석 내 백틱(`) 제거
- **ConvertStep2**: 다중 옵션 선택 변환 (EUCKR→UTF8, 백틱 제거)
- **ConvertStep2Comment**: 상세 주석 포함 변환
- **SimpleSourceTarget**: Source/Target 테이블 추출

### 2. SQL 파일 분석 (service.scan 패키지)
- **ScanSourceTarget**: SQL 파일에서 Source/Target 테이블 추출 및 리포트 생성
- **TableExtractor**: 테이블명 추출 (백틱 지원)
- **SqlFileScanner**: 디렉토리 재귀 스캔
- **FileReaderUtil**: 파일 읽기 유틸리티
- **ReportGenerator**: 리포트 생성

### 3. 공통 로그 모듈 (common.log 패키지)
- **SimpleAppLogger**: 순수 Java 로깅 유틸리티
  - 외부 라이브러리 불필요 (JDK만 필요)
  - 콘솔 + 파일 동시 출력
  - 일별 로그 파일 자동 생성
  - 에러 로그 별도 저장
  - 스레드 안전
  - 한글 완벽 지원

## 🚀 사용법

### 테이블 추출
```bash
javac -encoding UTF-8 -d target/classes -sourcepath src/main/java src/main/java/convert/SimpleSourceTarget.java
java -cp target/classes convert.SimpleSourceTarget
```

### SQL 파일 변환
```bash
javac -encoding UTF-8 -d target/classes -sourcepath src/main/java src/main/java/convert/ConvertStep1.java
java -cp target/classes com.cardgcp.ConvertStep1
```

### 다중 옵션 변환
```bash
javac -encoding UTF-8 -d target/classes -sourcepath src/main/java src/main/java/convert/ConvertStep2.java
java -cp target/classes convert.ConvertStep2
```

## 📚 문서

- [로그 모듈 사용 가이드](docs/logger-guide.md)
- [IntelliJ IDEA Run Dashboard 사용 가이드](docs/intellij-run-dashboard-guide.md)

## 💻 IntelliJ IDEA에서 실행하기

### 빠른 실행 방법
1. **Run Dashboard 열기**: `Alt + 5`
2. 실행할 애플리케이션 선택 (AppJob, AppStepJob, ScanSourceTarget 등)
3. 초록색 실행 버튼 클릭 또는 `Shift + F10`

자세한 내용은 [Run Dashboard 가이드](docs/intellij-run-dashboard-guide.md)를 참고하세요.
- [서비스 스캔 문서](docs/service-scan.md)

## 📁 프로젝트 구조

```
src/main/java/
├── com/log/             # 공통 로그 모듈
│   └── AppLogger.java
├── convert/             # SQL 변환 도구
│   ├── ConvertStep1.java
│   ├── ConvertStep2.java
│   ├── ConvertStep2Comment.java
│   └── SimpleSourceTarget.java
└── service/scan/        # SQL 분석 도구
    ├── io/              # 입출력
    │   ├── FileReaderUtil.java
    │   └── ReportGenerator.java
    ├── model/           # 데이터 모델
    │   └── TablesInfo.java
    ├── parser/          # 파서
    │   └── TableExtractor.java
    └── processor/       # 처리기
        ├── SqlFileProcessor.java
        └── SqlFileScanner.java
```

## 🔧 개발 환경

- Java 17+
- Maven (선택사항)
- Git

## 📝 로그 파일 위치

```
D:/11. Project/11. DB_OUT3/logs/
  ├── application-YYYY-MM-DD.log        # 일반 로그
  └── application-error-YYYY-MM-DD.log  # 에러 로그
```

## 🎯 특징

- ✅ 외부 라이브러리 의존성 없음 (순수 Java)
- ✅ Maven 없이도 javac로 직접 컴파일 가능
- ✅ 한글 테이블명 완벽 지원 (백틱 처리)
- ✅ 통합 로깅 시스템
- ✅ 객체지향 설계 (패키지별 기능 분리)

## 📄 라이선스

MIT License


