package service.compare.process;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 복합 키 전략: 지정된 컬럼들의 값을 결합하여 키 생성 */
public class CompositeKeyStrategy implements KeyStrategy {
    private static final char SEP = 0x1D; // Unit Separator: 충돌 최소화
    private final List<String> keyColumns;

    public CompositeKeyStrategy(List<String> keyColumns) {
        if (keyColumns == null || keyColumns.isEmpty()) {
            throw new IllegalArgumentException("키 컬럼이 비어 있습니다.");
        }
        this.keyColumns = keyColumns;
    }

    @Override
    public String buildKey(Map<String, String> rowValues) {
        return keyColumns.stream()
                .map(c -> {
                    String v = rowValues.get(c);
                    return v == null ? "" : v;
                })
                .collect(Collectors.joining(String.valueOf(SEP)));
    }

    @Override
    public String toString() {
        return "CompositeKey" + keyColumns;
    }
}

