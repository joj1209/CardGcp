package file.processor;

import file.parser.TableParser;
import file.vo.TablesInfo;

/**
 * 입력 SQL 문자열에서 Source/Target 테이블을 파싱하는 전용 Processor.
 */
public class FileParserProcessor {
    private final TableParser parser;

    public FileParserProcessor(TableParser parser) {
        this.parser = parser;
    }

    public static FileParserProcessor withDefaults() {
        return new FileParserProcessor(new TableParser());
    }

    public TablesInfo parse(String sql) {
        return parser.extractTables(sql);
    }
}