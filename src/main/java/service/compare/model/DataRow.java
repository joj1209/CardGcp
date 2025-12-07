package service.compare.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 한 행: 컬럼명 -> 값 */
public class DataRow {
    private final Map<String, String> values;

    public DataRow(Map<String, String> values) {
        // 보존된 순서 유지
        this.values = new LinkedHashMap<>(values);
    }

    public Map<String, String> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public String getValue(String column) {
        return values.get(column);
    }
}

