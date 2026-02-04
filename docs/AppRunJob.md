# AppRunJob - SQL 실행용 쿼리 생성 Job

## 개요
`AppRunJob`은 BigQuery SQL 파일을 분석하여 소스/타겟 테이블에 대한 실행 가능한 SQL 쿼리를 자동으로 생성하는 Job 클래스입니다.

## 주요 특징
- AppJob과 동일한 구조 (Reader → Processor → Writer 파이프라인)
- 출력 형태만 다름: 실행 가능한 SQL 쿼리 생성
- BigQuery 버전과 Oracle 버전 2개 파일 생성
- 기준일자를 파라미터로 받아 동적 쿼리 생성

## 구조

### 1. 클래스 구성
```
AppRunJob
├── SqlReader (Step 1: Read)
├── FileParserProcessor (Step 2: Parse)
└── SqlRunWriter (Step 3: Write)
```

### 2. 주요 구성 요소

#### SqlReader
- SQL 파일을 읽는 Reader
- UTF-8 인코딩으로 파일 읽기
- 파일 또는 디렉토리 모두 처리 가능

#### FileParserProcessor
- SQL 파일에서 소스/타겟 테이블 추출
- TableParser를 사용한 패턴 기반 파싱

#### SqlRunWriter (신규)
- 추출된 테이블 정보를 기반으로 실행 SQL 생성
- BigQuery 버전 (bq_*.sql)과 Oracle 버전 (ora_*.sql) 생성
- 각 테이블당 5개의 쿼리 자동 생성

## 출력 형식

### 생성되는 파일
각 입력 SQL 파일당 2개의 출력 파일 생성:
- `bq_<파일명>.sql` - BigQuery 버전 (백틱 포함)
- `ora_<파일명>.sql` - Oracle 버전 (백틱 제거)

### SQL 구조

```sql
/*--------------------*/
/*-- 소스테이블 : N개 --*/
/*--------------------*/

/*-- 1) 스키마.테이블명 --*/
select * from 스키마.`테이블명`;
select * from 스키마.`테이블명` where `파티션일자` = parse_date('%Y%m%d', '20260224');
select count(1) from 스키마.`테이블명`;
select `파티션일자`,count(1) from 스키마.`테이블명` group by `파티션일자` order by `파티션일자` desc;
select count(1) from 스키마.`테이블명` where `파티션일자` = parse_date('%Y%m%d', '20260224');

/*--------------------*/
/*-- 타겟테이블 : M개 --*/
/*--------------------*/

/*-- 1) 스키마.테이블명 --*/
(위와 동일한 5개 쿼리)
```

### 5개 쿼리의 목적
1. **전체 조회**: 테이블의 모든 데이터 조회
2. **기준일자 조회**: 특정 파티션 일자의 데이터만 조회
3. **전체 카운트**: 테이블의 총 레코드 수
4. **파티션별 카운트**: 파티션 일자별 레코드 수 및 정렬
5. **기준일자 카운트**: 특정 파티션 일자의 레코드 수

## 사용 방법

### 기본 실행 (기본 기준일자: 20260224)
```bash
java service.queryParser.job.AppRunJob
```

### 기준일자 지정
```bash
java service.queryParser.job.AppRunJob 20260225
```

### 아규먼트
- `args[0]`: 기준일자 (형식: YYYYMMDD, 기본값: 20260224)

## 코드 예시

### 입력 SQL 파일
```sql
-- STEP001
BEGIN
    INSERT INTO DM.`서비스1`
    SELECT * FROM DW.RED_CARE_SALES;
END;

-- STEP002
BEGIN
    MERGE INTO DM.`마스터가입자1` AS T
    USING DW.`서비스멤버1` AS S
    ON T.id = S.id;
END;
```

### 출력 파일 (bq_파일명.sql)
```sql
/*--------------------*/
/*-- 소스테이블 : 2개 --*/
/*--------------------*/

/*-- 1) DW.RED_CARE_SALES --*/
select * from DW.RED_CARE_SALES;
select * from DW.RED_CARE_SALES where `파티션일자` = parse_date('%Y%m%d', '20260224');
select count(1) from DW.RED_CARE_SALES;
select `파티션일자`,count(1) from DW.RED_CARE_SALES group by `파티션일자` order by `파티션일자` desc;
select count(1) from DW.RED_CARE_SALES where `파티션일자` = parse_date('%Y%m%d', '20260224');

/*-- 2) DW.서비스멤버1 --*/
select * from DW.`서비스멤버1`;
select * from DW.`서비스멤버1` where `파티션일자` = parse_date('%Y%m%d', '20260224');
select count(1) from DW.`서비스멤버1`;
select `파티션일자`,count(1) from DW.`서비스멤버1` group by `파티션일자` order by `파티션일자` desc;
select count(1) from DW.`서비스멤버1` where `파티션일자` = parse_date('%Y%m%d', '20260224');

/*--------------------*/
/*-- 타겟테이블 : 2개 --*/
/*--------------------*/

/*-- 1) DM.마스터가입자1 --*/
select * from DM.`마스터가입자1`;
select * from DM.`마스터가입자1` where `파티션일자` = parse_date('%Y%m%d', '20260224');
select count(1) from DM.`마스터가입자1`;
select `파티션일자`,count(1) from DM.`마스터가입자1` group by `파티션일자` order by `파티션일자` desc;
select count(1) from DM.`마스터가입자1` where `파티션일자` = parse_date('%Y%m%d', '20260224');

/*-- 2) DM.서비스1 --*/
select * from DM.`서비스1`;
select * from DM.`서비스1` where `파티션일자` = parse_date('%Y%m%d', '20260224');
select count(1) from DM.`서비스1`;
select `파티션일자`,count(1) from DM.`서비스1` group by `파티션일자` order by `파티션일자` desc;
select count(1) from DM.`서비스1` where `파티션일자` = parse_date('%Y%m%d', '20260224');
```

## AppJob과의 차이점

| 구분 | AppJob | AppRunJob |
|------|--------|-----------|
| **목적** | 테이블 추출 및 분석 | 실행 쿼리 생성 |
| **출력** | 텍스트 파일 + CSV | SQL 파일 (bq/ora) |
| **Writer** | TextWriter, CsvWriter | SqlRunWriter |
| **파일 수** | 파일당 3개 (txt + 2 csv) | 파일당 2개 (bq + ora) |
| **추가 기능** | CSV 통계 생성 | 기준일자 파라미터 |

## 백틱 처리 규칙

### BigQuery 버전
- 한글이 포함된 테이블명: 백틱 추가 (예: DW.`서비스1`)
- 영문 테이블명: 백틱 없음 (예: DW.RED_CARE_SALES)

### Oracle 버전
- 모든 백틱 제거
- 한글 테이블명: DW.서비스1
- 영문 테이블명: DW.RED_CARE_SALES

## 확장 가능성
- 기준일자 외에 추가 파라미터 확장 가능
- 쿼리 템플릿 커스터마이징 가능
- 다른 DBMS 버전 추가 가능 (예: PostgreSQL, MySQL)

## 관련 클래스
- `SqlRunWriter`: SQL 쿼리 생성 전담 Writer
- `AppJob`: 기본 분석 Job (참조 구조)
- `FileParserProcessor`: 테이블 파싱 프로세서
- `SqlReader`: SQL 파일 읽기

## 주의사항
1. 입력 SQL 파일은 UTF-8 인코딩 권장
2. 기준일자는 YYYYMMDD 형식으로 입력
3. 출력 디렉토리가 없으면 자동 생성됨
4. 동일 파일명이 있으면 덮어쓰기됨

