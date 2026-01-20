package service.queryParser.job;

import service.queryParser.processor.FileParserProcessor;
import service.queryParser.reader.SqlReader;
import service.queryParser.writer.CsvWriter;
import service.queryParser.writer.SourceTableCsvWriter;
import service.queryParser.writer.TextWriter;
import service.queryParser.vo.TablesInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SQL 파일에서 소스/타겟 테이블 정보를 추출하는 배치 작업 클래스입니다.
 *
 * <p>이 클래스는 Spring Batch 프레임워크의 Job 개념을 모방하여 설계되었으며,
 * Reader → Processor → Writer 세 단계의 파이프라인으로 구성됩니다.</p>
 *
 * <h3>처리 단계</h3>
 * <ul>
 *   <li><b>STEP1 (SqlReader)</b>: 지정된 디렉토리의 모든 .sql 파일을 순회하며 파일 내용을 읽습니다.
 *       <ul>
 *         <li>입력: SQL 파일이 저장된 디렉토리 경로</li>
 *         <li>출력: 각 파일의 경로와 내용(문자열)</li>
 *         <li>특징: 지정된 문자셋(기본: EUC-KR)으로 파일을 읽습니다.</li>
 *       </ul>
 *   </li>
 *   <li><b>STEP2 (FileParserProcessor)</b>: SQL 문자열을 파싱하여 소스/타겟 테이블 정보를 추출합니다.
 *       <ul>
 *         <li>입력: SQL 문자열</li>
 *         <li>출력: TablesInfo 객체 (소스 테이블 목록, 타겟 테이블 목록)</li>
 *         <li>특징: MERGE, INSERT, UPDATE, DELETE 등의 DML 문을 분석합니다.</li>
 *       </ul>
 *   </li>
 *   <li><b>STEP3 (TextWriter)</b>: 추출된 테이블 정보를 결과 파일로 저장합니다.
 *       <ul>
 *         <li>입력: TablesInfo 객체</li>
 *         <li>출력: 결과 텍스트 파일 (원본 파일명_out.txt)</li>
 *         <li>특징: 지정된 문자셋(기본: UTF-8)으로 파일을 작성합니다.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>
 * // 기본 설정으로 실행
 * AppJob job = AppJob.createDefault();
 * job.stepRead();
 *
 * // 커스텀 설정으로 실행
 * Path inputDir = Paths.get("D:", "myFolder");
 * SqlReader reader = new SqlReader(Charset.forName("UTF-8"));
 * FileParserProcessor processor = FileParserProcessor.withDefaults();
 * TextWriter writer = new TextWriter(Paths.get("D:", "output"), Charset.forName("UTF-8"));
 * AppJob customJob = new AppJob(inputDir, reader, processor, writer);
 * customJob.stepRead();
 * </pre>
 *
 * @see SqlReader 파일 입력 IO 담당
 * @see FileParserProcessor SQL 파싱 담당
 * @see TextWriter 파일 출력 IO 담당
 * @see TablesInfo 테이블 정보 저장 객체
 */
public class AppJob {

    // 기본 입력/출력 경로 (AppJob에서만 관리)
    private static final Path DEFAULT_INPUT_PATH = Paths.get("D:", "11. Project", "11. DB", "BigQuery");
    private static final Path DEFAULT_OUTPUT_PATH = Paths.get("D:", "11. Project", "11. DB", "BigQuery_out");

    /**
     * SQL 파일이 위치한 입력 디렉토리 경로입니다.
     * 이 디렉토리의 모든 .sql 파일이 처리 대상이 됩니다.
     */
    private final Path inputDir;

    /**
     * STEP1을 담당하는 SqlReader 인스턴스입니다.
     * 지정된 문자셋으로 SQL 파일을 읽어들입니다.
     */
    private final SqlReader reader;

    /**
     * STEP2를 담당하는 FileParserProcessor 인스턴스입니다.
     * SQL 문자열에서 소스/타겟 테이블 정보를 추출합니다.
     */
    private final FileParserProcessor processor;

    /**
     * STEP3을 담당하는 TextWriter 인스턴스입니다.
     * 추출된 테이블 정보를 결과 파일로 저장합니다.
     */
    private final TextWriter writer;

    /**
     * CSV 형식으로 전체 결과를 저장하는 CsvWriter 인스턴스입니다.
     */
    private final CsvWriter csvWriter;

    /**
     * 소스테이블 중심으로 프로그램 매핑 정보를 저장하는 SourceTableCsvWriter 인스턴스입니다.
     */
    private final SourceTableCsvWriter sourceTableCsvWriter;

    /**
     * AppJob 인스턴스를 생성합니다.
     *
     * <p>각 단계(Reader, Processor, Writer)를 외부에서 주입받아 유연하게 구성할 수 있습니다.
     * 이를 통해 다양한 입력 문자셋, 출력 디렉토리, 파싱 옵션 등을 설정할 수 있습니다.</p>
     *
     * @param inputDir SQL 파일이 위치한 입력 디렉토리 경로 (null 불가)
     * @param reader SQL 파일을 읽는 SqlReader 인스턴스 (null 불가)
     * @param processor SQL을 파싱하는 FileParserProcessor 인스턴스 (null 불가)
     * @param writer 결과를 출력하는 TextWriter 인스턴스 (null 불가)
     * @param csvWriter CSV로 결과를 출력하는 CsvWriter 인스턴스 (null 불가)
     * @param sourceTableCsvWriter 소스테이블 매핑 정보를 출력하는 SourceTableCsvWriter 인스턴스 (null 불가)
     */
    public AppJob(Path inputDir, SqlReader reader, FileParserProcessor processor, TextWriter writer,
                  CsvWriter csvWriter, SourceTableCsvWriter sourceTableCsvWriter) {
        this.inputDir = inputDir;
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
        this.csvWriter = csvWriter;
        this.sourceTableCsvWriter = sourceTableCsvWriter;
    }

    /**
     * 기본 설정으로 AppJob 인스턴스를 생성하는 팩토리 메서드입니다.
     *
     * <p>다음과 같은 기본 설정을 사용합니다:</p>
     * <ul>
     *   <li><b>입력 디렉토리</b>: D:\11. Project\11. DB\BigQuery</li>
     *   <li><b>입력 문자셋</b>: EUC-KR (SqlReader.DEFAULT_CHARSET)</li>
     *   <li><b>출력 디렉토리</b>: D:\11. Project\11. DB\BigQuery_out</li>
     *   <li><b>출력 문자셋</b>: UTF-8</li>
     *   <li><b>CSV 파일</b>: D:\11. Project\11. DB\BigQuery_out\summary.csv</li>
     *   <li><b>소스테이블 매핑 CSV 파일</b>: D:\11. Project\11. DB\BigQuery_out\source_table_mapping.csv</li>
     *   <li><b>파싱 옵션</b>: 기본 설정 (FileParserProcessor.withDefaults())</li>
     * </ul>
     *
     * <p>특정 경로나 문자셋을 사용하려면 생성자를 직접 호출하십시오.</p>
     *
     * @return 기본 설정이 적용된 AppJob 인스턴스
     */
    public static AppJob createDefault() {
        SqlReader reader = new SqlReader(SqlReader.DEFAULT_CHARSET);
        FileParserProcessor processor = FileParserProcessor.withDefaults();
        TextWriter writer = new TextWriter(DEFAULT_OUTPUT_PATH, Charset.forName("UTF-8"));
        Path csvPath = DEFAULT_OUTPUT_PATH.resolve("summary.csv");
        CsvWriter csvWriter = new CsvWriter(csvPath, Charset.forName("UTF-8"));
        Path sourceTableCsvPath = DEFAULT_OUTPUT_PATH.resolve("source_table_mapping.csv");
        SourceTableCsvWriter sourceTableCsvWriter = new SourceTableCsvWriter(sourceTableCsvPath, Charset.forName("UTF-8"));
        return new AppJob(DEFAULT_INPUT_PATH, reader, processor, writer, csvWriter, sourceTableCsvWriter);
    }

    /**
     * STEP1: SqlReader를 사용하여 입력 디렉토리의 모든 .sql 파일을 읽고 처리를 시작합니다.
     *
     * <p>이 메서드는 배치 파이프라인의 진입점으로, 다음 작업을 수행합니다:</p>
     * <ol>
     *   <li>inputDir 경로에 있는 모든 .sql 파일을 재귀적으로 탐색합니다.</li>
     *   <li>각 파일을 지정된 문자셋(기본: EUC-KR)으로 읽어들입니다.</li>
     *   <li>읽은 파일 경로와 내용을 {@link #handleFile(Path, String)} 메서드로 전달합니다.</li>
     *   <li>handleFile 메서드에서 STEP2(파싱)와 STEP3(출력)이 순차적으로 실행됩니다.</li>
     *   <li>모든 파일 처리가 완료되면 CSV 파일을 저장합니다.</li>
     * </ol>
     *
     * <p>처리 흐름:</p>
     * <pre>
     * stepRead()
     *   → SqlReader.run()
     *   → 각 파일마다 handleFile() 호출
     *   → stepParse() + stepWrite() + CSV 레코드 추가
     *   → 모든 파일 처리 완료 후 CSV 파일 저장
     * </pre>
     *
     * @see SqlReader#run(Path, SqlReader.SqlFileHandler)
     * @see #handleFile(Path, String)
     */
    public void stepRead() {
        System.out.println("========================================");
        System.out.println("Starting SQL file processing...");
        System.out.println("Input directory: " + inputDir);
        System.out.println("========================================");


        reader.run(inputDir, this::handleFile);

        // 모든 파일 처리 후 CSV 파일 저장
        try {
            csvWriter.write();
            System.out.println("========================================");
            System.out.println("CSV file saved successfully.");
            System.out.println("Total records: " + csvWriter.getRecordCount());
            System.out.println("========================================");
        } catch (IOException ex) {
            System.err.println("Failed to save CSV file: " + ex.getMessage());
        }

        // 소스테이블 매핑 CSV 파일 저장
        try {
            sourceTableCsvWriter.write();
            System.out.println("========================================");
            System.out.println("Source table mapping CSV file saved successfully.");
            System.out.println("Total source tables: " + sourceTableCsvWriter.getTableCount());
            System.out.println("========================================");
        } catch (IOException ex) {
            System.err.println("Failed to save source table mapping CSV file: " + ex.getMessage());
        }
    }

    /**
     * SqlReader가 읽어온 단일 SQL 파일을 처리하는 핵심 메서드입니다.
     *
     * <p>이 메서드는 STEP2(파싱)와 STEP3(출력)을 순차적으로 실행하며,
     * SqlReader의 콜백 함수로 각 파일마다 호출됩니다.</p>
     *
     * <h3>처리 순서</h3>
     * <ol>
     *   <li><b>STEP2 실행</b>: {@link #stepParse(String)}를 호출하여 SQL을 파싱하고
     *       소스/타겟 테이블 정보를 추출합니다.</li>
     *   <li><b>STEP3 실행</b>: {@link #stepWrite(Path, TablesInfo)}를 호출하여
     *       추출된 정보를 결과 파일로 저장합니다.</li>
     *   <li><b>CSV 레코드 추가</b>: CSV 파일에 레코드를 추가합니다.</li>
     * </ol>
     *
     * <h3>예외 처리</h3>
     * <p>파일 쓰기 과정에서 IOException이 발생하면 해당 파일의 처리를 건너뛰고
     * 에러 메시지를 표준 에러 스트림에 출력합니다. 다른 파일의 처리는 계속 진행됩니다.</p>
     *
     * @param file 원본 SQL 파일의 절대 경로 (예: D:\11. Project\11. DB\BigQuery\sample.sql)
     * @param sql 파일에서 읽어들인 SQL 문자열 전체 내용
     *
     * @see #stepParse(String)
     * @see #stepWrite(Path, TablesInfo)
     */
    private void handleFile(Path file, String sql) {
        try {
            TablesInfo info = stepParse(sql);
            stepWrite(file, info);

            // CSV 레코드 추가
            String fileName = file.getFileName().toString();
            csvWriter.addRecord(fileName, info);

            // 소스테이블 매핑 CSV 레코드 추가
            sourceTableCsvWriter.addRecord(fileName, info);
        } catch (IOException ex) {
            System.err.println("File processing failed: " + file + " - " + ex.getMessage());
        }
    }

    /**
     * STEP2: FileParserProcessor를 사용하여 SQL 문자열에서 소스/타겟 테이블 정보를 추출합니다.
     *
     * <p>이 단계에서는 SQL 문자열을 분석하여 다음 정보를 추출합니다:</p>
     * <ul>
     *   <li><b>타겟 테이블</b>: INSERT, UPDATE, DELETE, MERGE 등의 DML 문에서
     *       데이터가 변경되는 테이블 목록</li>
     *   <li><b>소스 테이블</b>: FROM, JOIN 절에서 참조되는 테이블 목록</li>
     * </ul>
     *
     * <h3>처리 특징</h3>
     * <ul>
     *   <li>STEP별 구조를 인식하여 각 단계의 테이블 정보를 별도로 관리</li>
     *   <li>백틱(`)으로 감싸진 테이블명과 일반 테이블명 모두 처리</li>
     *   <li>주석 내의 백틱은 제거하여 정확한 테이블명만 추출</li>
     *   <li>중복 테이블명 제거 (Set 자료구조 활용)</li>
     * </ul>
     *
     * @param sql 분석할 SQL 문자열 (여러 STEP을 포함할 수 있음)
     * @return TablesInfo 객체 (소스 테이블 Set, 타겟 테이블 Set 포함)
     *
     * @see FileParserProcessor#parse(String)
     * @see TablesInfo
     */
    private TablesInfo stepParse(String sql) {
        return processor.parse(sql);
    }

    /**
     * STEP3: TextWriter를 사용하여 파싱된 테이블 정보를 결과 파일로 저장합니다.
     *
     * <p>이 단계에서는 추출된 소스/타겟 테이블 정보를 지정된 형식으로 파일에 기록합니다.</p>
     *
     * <h3>출력 파일 형식</h3>
     * <ul>
     *   <li><b>파일명</b>: {원본파일명}_sql_tables.txt (예: sample.sql → sample_sql_tables.txt)</li>
     *   <li><b>저장 위치</b>: TextWriter에 지정된 출력 디렉토리 (기본: D:\11. Project\11. DB\BigQuery_out)</li>
     *   <li><b>문자셋</b>: UTF-8 (기본값)</li>
     * </ul>
     *
     * <h3>출력 내용 구조</h3>
     * <pre>
     * [Source Tables]
     * 스키마.테이블명1
     * 스��마.테이블명2
     * ...
     *
     * [Target Tables]
     * 스키마.테이블명3
     * 스키마.테이블명4
     * ...
     * </pre>
     *
     * @param file 원본 SQL 파일 경로 (출력 파일명 생성에 사용)
     * @param info 저장할 테이블 정보 (소스 테이블 목록, 타겟 테이블 목록)
     * @throws IOException 파일 쓰기 작업 중 I/O 오류가 발생한 경우
     *
     * @see TextWriter#writeTables(String, TablesInfo)
     * @see TablesInfo
     */
    private void stepWrite(Path file, TablesInfo info) throws IOException {
        String fileName = buildOutputFileName(file);
        writer.writeTables(fileName, info);
    }

    /**
     * 입력 파일명으로부터 출력 파일명을 생성합니다.
     *
     * @param file 입력 SQL 파일 경로
     * @return 출력 파일명 (예: sample_sql_tables.txt)
     */
    private String buildOutputFileName(Path file) {
        Path relative = inputDir.relativize(file);
        String name = relative.toString().replace("\\", "/");
        return name.replaceAll("\\.sql$", "_sql_tables.txt");
    }

    /**
     * 프로그램의 진입점입니다. 기본 설정으로 배치 작업을 실행합니다.
     *
     * <p>이 메서드는 다음 순서로 작업을 수행합니다:</p>
     * <ol>
     *   <li>{@link #createDefault()}를 호출하여 기본 설정의 AppJob 인스턴스를 생성합니다.</li>
     *   <li>{@link #stepRead()}를 호출하여 SQL 파일 읽기 및 처리를 시작합니다.</li>
     *   <li>입력 디렉토리의 모든 .sql 파일에 대해 파싱 및 출력 작업이 순차적으로 실행됩니다.</li>
     *   <li>모든 파일 처리 완료 후 CSV 요약 파일(summary.csv)이 생성됩니다.</li>
     * </ol>
     *
     * <h3>실행 방법</h3>
     * <pre>
     * java file.job.AppJob
     * </pre>
     *
     * <h3>커스텀 설정으로 실행하는 방법</h3>
     * <p>기본 설정 대신 특정 경로나 문자셋을 사용하려면 별도의 런처 클래스를 작성하거나
     * main 메서드를 수정하여 다음과 같이 사용할 수 있습니다:</p>
     * <pre>
     * Path customInput = Paths.get("D:", "custom", "input");
     * Path customOutput = Paths.get("D:", "custom", "output");
     * SqlReader customReader = new SqlReader(Charset.forName("UTF-8"));
     * FileParserProcessor customProcessor = FileParserProcessor.withDefaults();
     * TextWriter customWriter = new TextWriter(customOutput, Charset.forName("UTF-8"));
     * CsvWriter customCsvWriter = new CsvWriter(customOutput.resolve("summary.csv"), Charset.forName("UTF-8"));
     * AppJob customJob = new AppJob(customInput, customReader, customProcessor, customWriter, customCsvWriter);
     * customJob.stepRead();
     * </pre>
     *
     * @param args 명령줄 인자 (현재 사용되지 않음)
     *
     * @see #createDefault()
     * @see #stepRead()
     */
    public static void main(String[] args) {
        AppJob job = createDefault();
        job.stepRead();
    }
}