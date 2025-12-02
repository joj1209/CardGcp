package file.processor;

import file.parser.TableStepParser;
import file.vo.TablesInfo;

import java.util.Map;

/**
 * 입력 SQL 문자열을 STEP별로 분할하여 Source/Target 테이블을 파싱하는 전용 Processor.
 */
public class FileStepParserProcessor {
    private final TableStepParser parser;

    public FileStepParserProcessor(TableStepParser parser) {
        this.parser = parser;
    }

    public static FileStepParserProcessor withDefaults() {
        return new FileStepParserProcessor(new TableStepParser());
    }

    public Map<String, TablesInfo> parse(String sql) {
        return parser.extractTablesByStep(sql);
    }
}

