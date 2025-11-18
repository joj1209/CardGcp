# TableExtractor 테스트

SQL 파일에서 Source/Target 테이블을 추출하는 `TableExtractor` 클래스의 테스트 코드입니다.

## 테스트 클래스 목록

### 1. TableExtractorBasicTest
**기본 기능 테스트**
- INSERT INTO (Target 테이블)
- UPDATE (Target 테이블)
- DELETE FROM (Target 테이블)
- SELECT FROM (Source 테이블)
- JOIN (Source 테이블)
- 복잡한 쿼리 (INSERT INTO ... SELECT)

### 2. TableExtractorBacktickTest
**백틱이 있는 테이블명 추출 테스트**
- 백틱이 있는 테이블명 (`` `사원마스터` ``)
- 백틱 있음/없음 혼합
- 스키마명 + 백틱 (``mydb.`직원정보` ``)
- 한글 테이블명

### 3. TableExtractorKeywordFilterTest
**SQL 키워드 필터링 테스트**
- PostgreSQL 덤프 파일 키워드 (PUBLIC, CASCADE, STDIN)
- SQL 키워드가 테이블로 오인되는 경우 (DUAL, VALUES, SET)
- 실제 테이블명과 키워드 혼합
- 단일 문자 별칭(alias) 제외

### 4. TableExtractorTestRunner
**통합 테스트 실행기**
- 위의 모든 테스트를 순차적으로 실행

## 실행 방법

### 개별 테스트 실행

```bash
# 컴파일
javac -encoding UTF-8 -cp target/classes -d target/test-classes src/test/java/service/scan/parser/*.java

# 기본 기능 테스트
java -cp "target/classes;target/test-classes" service.scan.parser.TableExtractorBasicTest

# 백틱 테이블명 테스트
java -cp "target/classes;target/test-classes" service.scan.parser.TableExtractorBacktickTest

# SQL 키워드 필터링 테스트
java -cp "target/classes;target/test-classes" service.scan.parser.TableExtractorKeywordFilterTest
```

### 전체 테스트 실행

```bash
# 통합 테스트 실행
java -cp "target/classes;target/test-classes" service.scan.parser.TableExtractorTestRunner
```

## 테스트 결과 예시

```
========================================
TableExtractor 기본 기능 테스트
========================================

[테스트 1] INSERT INTO
  SQL: INSERT INTO emp_master (id, name) VALUES (1, 'John');
  Target: [emp_master]
  결과: ✅ PASS

[테스트 2] UPDATE
  SQL: UPDATE dept_master SET name = 'IT' WHERE id = 1;
  Target: [dept_master]
  결과: ✅ PASS

...

========================================
테스트 완료!
========================================
```

## 테스트 커버리지

### 지원하는 SQL 구문
- ✅ INSERT INTO
- ✅ UPDATE
- ✅ DELETE FROM
- ✅ MERGE INTO
- ✅ SELECT FROM
- ✅ JOIN (LEFT/RIGHT/INNER)

### 지원하는 테이블명 형식
- ✅ `table` - 일반 테이블명
- ✅ `` `table` `` - 백틱이 있는 테이블명
- ✅ `schema.table` - 스키마.테이블
- ✅ ``schema.`table` `` - 스키마.백틱테이블
- ✅ 한글 테이블명 (백틱 필수)

### 필터링 기능
- ✅ SQL 키워드 필터링 (30+ 키워드)
- ✅ 단일 문자 별칭 제외
- ✅ 주석 제거 (/* */)

## 참고사항

- 테스트 코드는 `src/test/java/service/scan/parser/` 경로에 위치합니다.
- 컴파일된 클래스는 `target/test-classes/` 경로에 생성됩니다.
- Windows 환경에서는 클래스패스 구분자로 `;`를 사용합니다.
- Linux/Mac 환경에서는 클래스패스 구분자로 `:`를 사용합니다.

