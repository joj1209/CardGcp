package service.compare.process;

import java.util.Map;

/** 키 생성 전략 */
public interface KeyStrategy {
    String buildKey(Map<String, String> rowValues);
}

