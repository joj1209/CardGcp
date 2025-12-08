# `service.scan` 모듈 개요

> SQL 파일에서 Source/Target 테이블을 추출하고 리포트를 생성하는 경량 파이프라인.

```
ScanSourceTarget (entry)
    └─ SqlFileScanner (walk *.sql)
         └─ SqlFileProcessor (per file)
              ├─ FileReaderUtil (I/O)
              ├─ TableExtractor (parser)
              ├─ TablesInfo (model)
              └─ ReportGenerator (output)
```

---

## 1. 엔트리 포인트: `service.scanSourceTarget.ScanSourceTarget`
| 목적 | SRC_ROOT(`D:\11. Project\11. DB`) 아래 *.sql 전부 스캔해 OUT_ROOT(`D:\11. Project\11. DB_OUT3`)에 리포트 작성 |
|------|--------------------------------------------------------------------------------------------------------------|
| 핵심 흐름 | 입력·출력 경로 검증 → `SqlFileProcessor` / `SqlFileScanner` 생성 → `scanDirectory` 호출 |
| 오류 처리 | 입력 폴더 미존재 시 즉시 예외, 개별 파일 오류는 로그만 출력하고 계속 진행 |

---

## 2. Processor + Scanner 계층
### `service.scan.processor.SqlFileScanner`
- `scanDirectory(Path root)` : `Files.walkFileTree`로 하위 디렉토리 전체 순회
- `.sql` 확장자 필터 (소문자 비교) 후 `SqlFileProcessor#processFile` 호출
- 실패 파일은 STDERR에 로깅만 하고 계속 진행 → 전수 스캔 유지

### `service.scan.processor.SqlFileProcessor`
| 구성요소 | 역할 |
|----------|------|
| `Path srcRoot/outRoot` | 상대 경로 계산, 결과 파일 경로 생성 |
| `TableExtractor` | SQL 문자열에서 Source/Target 추출 |
| `FileReaderUtil` | (기본 MS949) 파일 전체 로드, 오류 문자 REPLACE |
| `ReportGenerator` | 추출 결과를 텍스트 리포트로 직렬화 |

주요 메서드
1. `processFile(Path sqlFile)` : 읽기 → 추출 → 리포트 문자열 생성 → `resolveOutFile` → UTF-8로 저장
2. `resolveOutFile(Path sqlFile)` : `srcRoot` 대비 상대 경로 유지하며 `.source_target.txt` 파일명으로 매핑
3. `write(Path out, String s)` : `CREATE/TRUNCATE` 모드로 결과 저장

---

## 3. 파서 & 모델 계층
### `service.scan.parser.TableExtractor`
- 테이블 식별자 패턴: `스키마.테이블` 또는 백틱 포함 이름 (한글 컬럼 지원)
- 지원 DML 키워드
  - Target: `INSERT INTO`, `UPDATE`, `MERGE INTO`, `DELETE FROM`
  - Source: `FROM`, `JOIN`
- 블록 주석(`/* ... */`) 제거 후 정규식 반복 매칭
- 후처리: `clean()`으로 콤마/세미콜론/괄호 제거, trim
- 결과는 `TablesInfo` 세트에 중복 없이 누적

### `service.scan.model.TablesInfo`
- 두 개의 `LinkedHashSet<String>` (`sources`, `targets`) 보관 → 입력 순서 유지
- `addSource/addTarget`가 null/빈 문자열 필터 처리
- `isEmpty()`로 리포트 생략 여부 판단

---

## 4. I/O 계층
### `service.scan.io.FileReaderUtil`
- 기본 문자셋 `MS949`, 필요 시 생성자에서 대체 가능
- `CharsetDecoder`를 `CodingErrorAction.REPLACE`로 구성해 깨진 문자 최소화

### `service.scan.io.ReportGenerator`
- 출력 형식
  ```
  FILE: <절대경로>
  [Target Tables]
    1. 대상테이블
  [Source Tables]
    1. 소스타
  ```
- 대상/소스 각각 존재할 때만 섹션 생성, 없으면 `(추출된 테이블 없음)` 메시지

---

## 5. 활용 예시
```java
Path SRC = Paths.get("D:/11. Project/11. DB");
Path OUT = Paths.get("D:/11. Project/11. DB_OUT3");
SqlFileProcessor processor = new SqlFileProcessor(SRC, OUT);
SqlFileScanner scanner = new SqlFileScanner(processor);
int processed = scanner.scanDirectory(SRC);
```

- 결과 파일은 입력 디렉토리 구조를 보존한 채 `*.source_target.txt` 로 OUT_ROOT에 생성
- 같은 테이블이 Target/Souce 모두 등장해도 각각의 세트에 기록됨

---

## 6. 확장 포인트
1. **문자셋 설정**: `FileReaderUtil` 생성자 인자로 UTF-8 등 다른 인코딩 주입
2. **패턴 추가**: `TableExtractor`에 `Pattern` 추가/교체 후 `findTables` 호출
3. **보고서 포맷 변경**: `ReportGenerator` 커스터마이즈 (Markdown, CSV 등)
4. **병렬 처리**: `SqlFileProcessor`를 스레드 안전하게 만들고 `SqlFileScanner`에서 Executor 사용 가능

