package service.fileUtil.processor;

public class ConvertStep {

    public String process(String content) {
        // 현재는 그대로 반환
        // 추후 여기에 변환 로직 추가 가능:
        // - SQL 문법 변환
        // - 주석 제거/변환
        // - 포맷팅
        // - 특정 문자열 치환 등
        return content;
    }

    public String removeTrailingSpaces(String content) {
        if (content == null) return null;
        return java.util.Arrays.stream(content.split("\n", -1))
                .map(line -> line.replaceAll("\\s+$", ""))
                .collect(java.util.stream.Collectors.joining("\n"));
    }
}
