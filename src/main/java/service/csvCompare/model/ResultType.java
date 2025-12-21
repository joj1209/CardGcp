package service.csvCompare.model;

/** 행 비교 결과 유형 */
public enum ResultType {
    MATCHED,
    MISMATCHED,
    ONLY_IN_FILE1,
    ONLY_IN_FILE2
}
