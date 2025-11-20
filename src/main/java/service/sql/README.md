# service.sql 모듈

순수 자바 환경에서도 실행 가능한 SQL → CSV 유틸리티입니다.

## 환경 변수 / 시스템 프로퍼티

| 키 | 설명 |
| --- | --- |
| `APP_SQL_JDBC_URL` | JDBC URL (예: `jdbc:mysql://127.0.0.1:3306/sample`) |
| `APP_SQL_USERNAME` | DB 사용자 |
| `APP_SQL_PASSWORD` | DB 비밀번호 |
| `APP_SQL_SCRIPT_PATH` | 실행할 SQL 파일 경로 |
| `APP_SQL_OUTPUT_DIR` | 결과 CSV 저장 폴더 |
| `APP_SQL_EXCEL_NAME` | 결과 파일명 (기본값 `result.xlsx`) |

동일한 값을 JVM 옵션(`-Dapp.sql.jdbc-url=...`)으로 전달해도 됩니다.

## 실행 방법

```powershell
cd "D:\11. Project\02. Java\backend\CardGcp"
# 필요한 환경 변수 설정 후
java -cp target/CardGcp-0.0.1-SNAPSHOT.jar service.sql.SqlRunner
```

## 동작 요약

1. `SqlRunner` 에서 환경 설정을 읽고 JDBC 커넥션을 연다.
2. `SqlFileExecutor` 가 SQL 파일을 문장 단위로 실행하여 SELECT 결과를 수집한다.
3. `ExcelResultExporter` 가 결과를 CSV 파일로 저장한다.

## 주의 사항

- SQL 파일은 UTF-8 기준이며, 다른 인코딩의 경우 `SqlFileExecutor` 수정 필요.
- SELECT 결과가 없으면 예외가 발생한다.
- JDBC 드라이버 JAR 은 `java -cp` 실행 시 포함시켜야 한다.

