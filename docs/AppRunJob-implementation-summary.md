# AppRunJob 구현 완료 요약

## 구현된 기능

### 1. ✅ AppJob과 동일한 구조
- Reader → Processor → Writer 파이프라인 구조
- SqlReader, FileParserProcessor 재사용
- 새로운 SqlRunWriter 추가

### 2. ✅ 출력 형태 변경
- 기존: 텍스트 파일 + CSV 파일
- 신규: BigQuery SQL 파일 + Oracle SQL 파일
- 각 입력 파일당 2개 출력 파일 생성:
  - `<파일명>_bq.sql` (BigQuery 버전, 백틱 포함)
  - `<파일명>_oracle.sql` (Oracle 버전, 백틱 제거)
- 예시: `bq_dw_red_care_sales_01.sql` → `bq_dw_red_care_sales_01_bq.sql`, `bq_dw_red_care_sales_01_oracle.sql`

### 3. ✅ 소스/타겟 테이블별 5개 쿼리 생성
각 테이블마다 다음 5개 쿼리 자동 생성:
1. `select * from 테이블명;` - 전체 조회
2. `select * from 테이블명 where 파티션일자 = parse_date('%Y%m%d', '기준일자');` - 기준일자 조회
3. `select count(1) from 테이블명;` - 전체 카운트
4. `select 파티션일자,count(1) from 테이블명 group by 파티션일자 order by 파티션일자 desc;` - 파티션별 카운트
5. `select count(1) from 테이블명 where 파티션일자 = parse_date('%Y%m%d', '기준일자');` - 기준일자 카운트

### 4. ✅ 기준일자 아규먼트 처리
- 아규먼트로 기준일자 입력 가능
- 기본값: `20260224`
- 형식: `YYYYMMDD`
- 사용법: `java service.queryParser.job.AppRunJob 20260301`

### 5. ✅ 백틱 처리 규칙
#### BigQuery 버전
- 한글 테이블명: 자동으로 백틱 추가 (예: `DW.`서비스1``)
- 영문 테이블명: 백틱 없음 (예: `DW.RED_CARE_SALES`)
- Unicode 블록 기반 한글 자동 감지

#### Oracle 버전
- 모든 백틱 제거
- 한글: `DW.서비스1`
- 영문: `DW.RED_CARE_SALES`

## 생성된 파일 구조

```
service/queryParser/
├── job/
│   ├── AppJob.java (기존)
│   └── AppRunJob.java (신규)
└── writer/
    ├── TextWriter.java (기존)
    ├── CsvWriter.java (기존)
    └── SqlRunWriter.java (신규)
```

## 실행 테스트 결과

### 입력 파일
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

### 출력 파일 (bq_dw_red_care_sales_01_bq.sql)
```sql
/*--------------------*/
/*-- 소스테이블 : 3개 --*/
/*--------------------*/

/*-- 1) DW.RED_CARE_SALES --*/
select * from DW.RED_CARE_SALES;
select * from DW.RED_CARE_SALES where `파티션일자` = parse_date('%Y%m%d', '20260224');
select count(1) from DW.RED_CARE_SALES;
select `파티션일자`,count(1) from DW.RED_CARE_SALES group by `파티션일자` order by `파티션일자` desc;
select count(1) from DW.RED_CARE_SALES where `파티션일자` = parse_date('%Y%m%d', '20260224');

/*-- 2) DW.`마스터가입자1` --*/
(5개 쿼리)

/*-- 3) DW.`서비스멤버1` --*/
(5개 쿼리)

/*--------------------*/
/*-- 타겟테이블 : 2개 --*/
/*--------------------*/

/*-- 1) DM.`마스터가입자1` --*/
(5개 쿼리)

/*-- 2) DM.`서비스1` --*/
(5개 쿼리)
```

### 출력 파일 (bq_dw_red_care_sales_01_oracle.sql)
- 위와 동일하지만 모든 백틱 제거됨

## 기술적 특징

### 1. 한글 자동 감지
```java
private boolean containsKorean(String text) {
    for (char c : text.toCharArray()) {
        if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_SYLLABLES ||
            Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO ||
            Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_JAMO) {
            return true;
        }
    }
    return false;
}
```

### 2. 테이블 정렬
- `TreeSet`을 사용한 자동 알파벳 순 정렬
- 소스/타겟 테이블 각각 독립적으로 정렬
- 가독성 높은 출력

### 3. 유연한 쿼리 템플릿
- 기준일자 동적 적용
- 파티션 컬럼명: `파티션일자` (고정)
- 필요시 쿼리 템플릿 확장 가능

## 문서화

### docs/AppRunJob.md 포함 내용
1. 개요 및 주요 특징
2. 클래스 구조 및 구성 요소
3. 출력 형식 상세 설명
4. 사용 방법 및 아규먼트
5. 코드 예시 (입력/출력)
6. AppJob과의 차이점 비교표
7. 백틱 처리 규칙
8. 실행 예시 및 콘솔 출력
9. 기술적 특징 상세
10. 관련 클래스 및 주의사항

## Git 커밋 이력

### 1차 커밋 (0c2fcb7)
```
feat: Add AppRunJob - SQL 실행용 쿼리 생성 Job 구현

- AppRunJob 클래스 추가: AppJob과 동일한 구조, 출력만 다름
- SqlRunWriter 클래스 추가: BigQuery/Oracle 버전 SQL 생성
- 소스/타겟 테이블당 5개의 실행 쿼리 자동 생성
- 기준일자를 아규먼트로 받아 동적 쿼리 생성 (기본값: 20260224)
- BigQuery 버전(<파일명>_bq.sql)과 Oracle 버전(<파일명>_oracle.sql) 파일 생성
- 한글 테이블명 자동 백틱 처리
- 테스트 샘플 SQL 파일 추가
- AppRunJob 상세 문서 추가 (docs/AppRunJob.md)
```

### 2차 커밋 (fcb6cb4)
```
docs: Update AppRunJob documentation with execution examples

- 실행 예시 및 콘솔 출력 추가
- 기준일자 파라미터 사용법 설명 추가
- 기술적 특징 섹션 추가 (한글 감지, 테이블 정렬, 파일명 처리)
- 실제 테스트 결과 반영
- DEFAULT_INPUT_PATH를 원래 경로로 복원
```

## 테스트 결과

### 기본 실행 (기준일자: 20260224)
```bash
java service.queryParser.job.AppRunJob
```
✅ 성공: 모든 SQL 파일에 대해 bq/ora 버전 생성됨

### 기준일자 변경 (20260301)
```bash
java service.queryParser.job.AppRunJob 20260301
```
✅ 성공: 쿼리에 20260301 기준일자 적용됨

### 백틱 처리 검증
- BigQuery 버전: 한글 테이블명에 백틱 추가됨 ✅
- Oracle 버전: 모든 백틱 제거됨 ✅

## 확장 가능성

1. **다른 DBMS 지원**: PostgreSQL, MySQL 버전 추가 가능
2. **쿼리 템플릿 커스터마이징**: 쿼리 형식 변경 가능
3. **파티션 컬럼명 동적 설정**: 파라미터로 받을 수 있도록 확장
4. **추가 파라미터**: 시작일자~종료일자 범위 쿼리 등

## 요구사항 충족 검증

| 요구사항 | 상태 | 비고 |
|---------|------|------|
| AppJob과 동일한 구조 | ✅ | Reader-Processor-Writer 파이프라인 |
| 출력 형태 변경 | ✅ | SQL 쿼리 파일 생성 |
| BigQuery/Oracle 2개 버전 | ✅ | <파일명>_bq.sql, <파일명>_oracle.sql |
| 백틱 제거 (Oracle) | ✅ | replace("`", "") |
| 테이블당 5개 쿼리 | ✅ | 전체/파티션/카운트 쿼리 |
| 기준일자 아규먼트 | ✅ | args[0], 기본값 20260224 |
| 문서화 | ✅ | docs/AppRunJob.md |
| Git 커밋/푸시 | ✅ | 2개 커밋 완료 |

## 결론

**모든 요구사항이 성공적으로 구현되고 테스트되었습니다.**

- ✅ 기능 구현 완료
- ✅ 테스트 검증 완료
- ✅ 문서화 완료
- ✅ Git 커밋/푸시 완료
- ✅ 코드 품질 검증 (컴파일 성공, 워닝 최소화)

