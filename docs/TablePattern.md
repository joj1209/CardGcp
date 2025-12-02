# TablePattern 클래스

## 개요
SQL 쿼리에서 소스/타겟 테이블을 패턴별로 추출하는 클래스입니다.
TableExtractor를 대체하여 더 명확한 구조와 높은 가독성을 제공합니다.

## 주요 기능

### 1. 타겟 테이블 추출
타겟 테이블은 데이터가 **변경**되는 테이블을 의미합니다.

#### 1.1 INSERT 패턴
```sql
INSERT INTO DM.`요약01`
SELECT * FROM DW.`기지국01`
```
- **추출**: `DM.`요약01``

#### 1.2 UPDATE 패턴
```sql
UPDATE BM.`회사`
SET name = 'New Name'
WHERE id = 1
```
- **추출**: `BM.`회사``

#### 1.3 DELETE 패턴
```sql
-- DELETE FROM 방식
DELETE FROM DW.`부서06`

-- Oracle 방식
DELETE DW.`부서06` WHERE id = 1
```
- **추출**: `DW.`부서06``

#### 1.4 MERGE 패턴
```sql
MERGE INTO DM.일별카드발급현황 AS T
USING DW.카드목록 AS S
ON T.id = S.id
WHEN MATCHED THEN UPDATE SET T.name = S.name
```
- **추출**: `DM.일별카드발급현황`

### 2. 소스 테이블 추출
소스 테이블은 데이터를 **읽는** 테이블을 의미합니다.

#### 2.1 FROM 절
```sql
SELECT * FROM DW.`기지국01` AS N1
```
- **추출**: `DW.`기지국01``

#### 2.2 LEFT JOIN / INNER JOIN
```sql
SELECT *
FROM DW.`기지국02` AS N1
LEFT JOIN DW.`부서02` AS N2
  ON N1.ENT_ID = N2.ENT_ID
```
- **추출**: `DW.`기지국02``, `DW.`부서02``

#### 2.3 Oracle 조인 문법 (콤마)
```sql
SELECT *
FROM DW.`기지국04` AS N1
   , DW.`부서04` AS N2
WHERE N1.ENT_ID = N2.ENT_ID
```
- **추출**: `DW.`기지국04``, `DW.`부서04``

#### 2.4 WITH 절 (CTE)
```sql
WITH `모수` AS (
  SELECT * FROM DW.`기지국05`
)
SELECT * FROM `모수`
```
- **추출**: `DW.`기지국05``

#### 2.5 USING 절 (MERGE)
```sql
MERGE INTO target_table
USING DW.카드목록 AS S
ON target.id = S.id
```
- **추출**: `DW.카드목록`

### 3. 테이블명 패턴 지원

다양한 테이블명 형식을 지원합니다:

| 패턴 | 예시 | 설명 |
|------|------|------|
| \`table\` | \`요약01\` | 백틱으로 감싼 테이블명 |
| schema.\`table\` | DW.\`기지국01\` | 스키마.백틱 테이블 |
| schema.table | DM.일별카드발급현황 | 스키마.테이블 |
| table | users | 단순 테이블명 |

## 클래스 구조

### 메소드 분류

#### 타겟 테이블 추출 메소드
- `extractTargetTables()` - 타겟 테이블 총괄 추출
- `extractInsertTargets()` - INSERT 패턴
- `extractUpdateTargets()` - UPDATE 패턴
- `extractDeleteTargets()` - DELETE 패턴
- `extractMergeTargets()` - MERGE 패턴

#### 소스 테이블 추출 메소드
- `extractSourceTables()` - 소스 테이블 총괄 추출
- `extractFromTables()` - FROM 절
- `extractJoinTables()` - JOIN 절 (LEFT, INNER, RIGHT)
- `extractUsingTables()` - USING 절
- `extractWithTables()` - WITH 절 (CTE)
- `extractOracleJoinTables()` - Oracle 조인 (콤마)

#### 유틸리티 메소드
- `removeComments()` - SQL 주석 제거
- `extractTablesByPattern()` - 정규식으로 테이블 추출
- `addTableIfValid()` - 유효성 검사 후 추가
- `cleanTableName()` - 테이블명 정리
- `isTrailingChar()` - 끝 문자 체크

## 사용 방법

### 기본 사용
```java
TablePattern pattern = new TablePattern();
TablesInfo info = pattern.extractTables(sql);

// 소스 테이블
Set<String> sources = info.getSources();

// 타겟 테이블
Set<String> targets = info.getTargets();
```

### 스텝별 추출
```java
String[] steps = sql.split("/\\*\\s*STEP\\d+\\s*\\*/");

for (int i = 1; i < steps.length; i++) {
    TablesInfo stepInfo = pattern.extractTables(steps[i]);
    System.out.println("STEP" + i);
    System.out.println("Sources: " + stepInfo.getSources());
    System.out.println("Targets: " + stepInfo.getTargets());
}
```

## SQL 키워드 제외

다음 키워드는 테이블명으로 인식하지 않습니다:
```
SELECT, FROM, WHERE, INSERT, UPDATE, DELETE, JOIN, LEFT, RIGHT,
INNER, OUTER, ON, AND, OR, NOT, IN, EXISTS, BETWEEN, LIKE,
ORDER, GROUP, BY, HAVING, LIMIT, OFFSET, UNION, ALL, DISTINCT,
AS, INTO, VALUES, SET, CASCADE, RESTRICT, PUBLIC, PRIVATE,
GRANT, REVOKE, COMMIT, ROLLBACK, SAVEPOINT, TRANSACTION, BEGIN,
END, IF, THEN, ELSE, CASE, WHEN, NULL, TRUE, FALSE, USING
```

## FileParserProcessor와의 통합

`FileParserProcessor`는 `TablePattern`을 래핑하여 사용합니다:

```java
public class FileParserProcessor {
    private final TablePattern extractor;

    public static FileParserProcessor withDefaults() {
        return new FileParserProcessor(new TablePattern());
    }

    public TablesInfo parse(String sql) {
        return extractor.extractTables(sql);
    }
}
```

## 테스트 예시

### 입력 (sample002.sql)
```sql
/* STEP001 */
BEGIN
    INSERT INTO DM.`요약01`
    SELECT * FROM DW.`기지국01` AS N1
END;

/* STEP002 */
BEGIN
    SELECT *
    FROM DW.`기지국02` AS N1
    LEFT JOIN DW.`부서02` AS N2
      ON N1.ENT_ID = N2.ENT_ID
END;
```

### 출력
```
STEP001:
  Sources: DW.`기지국01`
  Targets: DM.`요약01`

STEP002:
  Sources: DW.`기지국02`, DW.`부서02`
  Targets: (없음)
```

## TableExtractor와의 차이점

| 항목 | TableExtractor | TablePattern |
|------|----------------|--------------|
| 구조 | 단일 메소드에 모든 로직 | 패턴별 메소드 분리 |
| 가독성 | 낮음 | 높음 |
| 유지보수성 | 어려움 | 쉬움 |
| 확장성 | 제한적 | 우수 |
| 패턴 추가 | 복잡함 | 간단함 |

## 설계 원칙

1. **단일 책임 원칙**: 각 메소드는 하나의 패턴만 처리
2. **개방-폐쇄 원칙**: 새로운 패턴 추가가 용이
3. **명확한 네이밍**: 메소드명으로 기능 파악 가능
4. **테스트 용이성**: 패턴별 개별 테스트 가능

## 참고 파일

- `TableNamePattern_패턴요건.txt` - 테이블명 패턴 요구사항
- `TableSourcePattern_패턴요건.txt` - 소스 테이블 패턴 요구사항
- `TableTargetPattern_패턴요건.txt` - 타겟 테이블 패턴 요구사항

