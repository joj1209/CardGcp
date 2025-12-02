package file.processor;

import file.vo.TablesInfo;
import service.scan.parser.TableExtractor;

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
        service.scan.model.TablesInfo raw = extractor.extractTables(sql);
        return convert(raw);
    }

    private TablesInfo convert(service.scan.model.TablesInfo raw) {
        TablesInfo info = new TablesInfo();
        raw.getSources().forEach(info::addSource);
        raw.getTargets().forEach(info::addTarget);
        return info;
    }
}
