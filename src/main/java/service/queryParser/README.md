# service.queryParser 패키지

SQL 스크립트 파일에서 소스 테이블과 타겟 테이블을 자동으로 추출하는 배치 처리 시스템입니다.

## 📦 프로젝트 정보

- **Git 저장소**: https://github.com/joj1209/CardGcp.git
- **패키지 경로**: `src/main/java/service/file/`
- **문서 위치**: `docs/service-file-audio-guide.md`

## 🚀 시작하기

### 저장소 클론
```bash
git clone https://github.com/joj1209/CardGcp.git
cd CardGcp
```

### 빠른 실행
```java
// 기본 설정으로 실행
AppJob job = AppJob.createDefault();
job.stepRead();
```

## 📚 패키지 구조

```
service.queryParser/
├── job/                  # 작업 조율 (AppJob, AppStepJob)
├── reader/               # 파일 입력 (SqlReader)
├── processor/            # SQL 파싱 (FileParserProcessor, FileStepParserProcessor)
├── parser/               # 파싱 로직 (TableParser, TableStepParser)
├── pattern/              # 정규식 패턴 (TableNamePattern, TableSourcePattern, TableTargetPattern)
├── writer/               # 결과 저장 (TextWriter, TextStepWriter, CsvWriter)
└── vo/                   # 데이터 객체 (TablesInfo)
```

## 🎯 주요 기능

- ✅ INSERT, UPDATE, DELETE, MERGE 등 DML 문 인식
- ✅ 다양한 JOIN 문법 지원 (INNER, LEFT, RIGHT, Oracle 조인)
- ✅ WITH 절 CTE 처리
- ✅ STEP별 SQL 분석
- ✅ 다양한 인코딩 지원 (UTF-8, EUC-KR)
- ✅ 텍스트 및 CSV 결과 출력

## 📖 상세 문서

### 학습 가이드
출퇴근하면서 들으며 학습할 수 있는 상세 가이드:
- [service-file-audio-guide.md](../../../docs/service-file-audio-guide.md)

### 클래스별 문서
- [AppJob 상세 설명](../../../docs/AppJob.md)
- [SqlReader 상세 설명](../../../docs/SqlReader.md)
- [TableStepParser 상세 설명](../../../docs/TableStepParser.md)
- [테이블 패턴 시스템](../../../docs/table-pattern-system.md)

## 💻 사용 예시

### 1. 전체 SQL 파일 분석
```java
// 기본 경로: D:\11. Project\11. DB\BigQuery
AppJob job = AppJob.createDefault();
job.stepRead();
// 결과: BigQuery_out 폴더에 텍스트 파일 + summary.csv
```

### 2. STEP별 분석
```java
AppStepJob job = AppStepJob.createJob();
job.execute();
// 결과: 각 STEP마다 소스/타겟 테이블 구분
```

### 3. 커스텀 설정
```java
Path inputPath = Paths.get("D:", "myFolder");
SqlReader reader = new SqlReader(Charset.forName("EUC-KR"));
FileParserProcessor processor = FileParserProcessor.withDefaults();
TextWriter writer = new TextWriter(outputPath, Charset.forName("UTF-8"));
CsvWriter csvWriter = new CsvWriter(csvPath, Charset.forName("UTF-8"));
AppJob job = new AppJob(inputPath, reader, processor, writer, csvWriter);
job.stepRead();
```

## 🔧 기술 스택

- Java 8
- 순수 Java (외부 의존성 없음)
- 정규식 기반 SQL 파싱
- Spring Batch 유사 아키텍처

## 📝 라이선스

이 프로젝트는 CardGcp 프로젝트의 일부입니다.

## 👥 기여

문제나 개선사항이 있으면 이슈를 등록해주세요:
https://github.com/joj1209/CardGcp/issues

## 📧 문의

프로젝트 관련 문의: https://github.com/joj1209

---

**마지막 업데이트**: 2025-12-08

