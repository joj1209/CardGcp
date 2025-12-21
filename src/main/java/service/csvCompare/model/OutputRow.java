package service.csvCompare.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 출력용 행: 값 + RESULT + DIFF_DETAIL */
public class OutputRow {
    private final Map<String, String> valuesInUnifiedHeaderOrder;
    private final ResultType resultType;
    private final String diffDetail;

    public OutputRow(Map<String, String> baseValues, ResultType resultType, String diffDetail) {
        // baseValues는 이미 unifiedHeaders 순서로 구성되었다고 가정
        this.valuesInUnifiedHeaderOrder = new LinkedHashMap<>(baseValues);
        this.resultType = resultType;
        this.diffDetail = diffDetail == null ? "" : diffDetail;
    }

    public List<String> toOutputFields() {
        List<String> fields = new ArrayList<>(valuesInUnifiedHeaderOrder.values());
        fields.add(resultType.name());
        fields.add(diffDetail);
        return fields;
    }
}

