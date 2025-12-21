package service.csvCompare.model;

import java.util.Collections;
import java.util.List;

/** 헤더 + 데이터 행들의 테이블 */
public class CsvTable {
    private final List<String> headers;
    private final List<DataRow> rows;

    public CsvTable(List<String> headers, List<DataRow> rows) {
        this.headers = headers;
        this.rows = rows;
    }

    public List<String> getHeaders() { return Collections.unmodifiableList(headers); }
    public List<DataRow> getRows() { return Collections.unmodifiableList(rows); }
}
