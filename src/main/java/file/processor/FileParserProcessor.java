package file.processor;

import file.parser.TableExtractor;
import file.vo.TablesInfo;

/**
 * 입력 SQL 문자열에서 Source/Target 테이블을 파싱하는 전용 Processor.
 */
public class FileParserProcessor {
    private final TableExtractor extractor;

    public FileParserProcessor(TableExtractor extractor) {
        this.extractor = extractor;
    }

    public static FileParserProcessor withDefaults() {
        return new FileParserProcessor(new TableExtractor());
    }

    public TablesInfo parse(String sql) {
        return extractor.extractTables(sql);
    }
}
