package test;

import java.util.function.Function;
import java.util.function.BiFunction;

public class MethodReferenceExample {

    public static void main(String[] args) {
        MethodReferenceExample example = new MethodReferenceExample();

        // ============================================
        // Function<T, R>: 파라미터 1개
        // ============================================
        System.out.println("=== Function<String, String> 예시 ===");

        // 방법 1: 람다 표현식
        Function<String, String> func1 = (input) -> example.process(input);

        // 방법 2: 메서드 참조 (같은 의미!)
        Function<String, String> func2 = example::process;

        // 사용
        String result1 = func1.apply("Hello World  ");
        String result2 = func2.apply("Hello World  ");

        System.out.println("Lambda result: [" + result1 + "]");
        System.out.println("Method ref result: [" + result2 + "]");


        // ============================================
        // BiFunction<T, U, R>: 파라미터 2개 (비교용)
        // ============================================
        System.out.println("\n=== BiFunction<String, String, String> 예시 (파라미터 2개) ===");

        // 파라미터가 2개인 메서드는 BiFunction 사용
        BiFunction<String, String, String> biFunc1 = (a, b) -> example.concat(a, b);
        BiFunction<String, String, String> biFunc2 = example::concat;

        String result3 = biFunc1.apply("Hello", "World");
        String result4 = biFunc2.apply("Hello", "World");

        System.out.println("BiFunction result: " + result3);
        System.out.println("Method ref result: " + result4);


        // ============================================
        // 실제 ConvertStep 메서드 시뮬레이션
        // ============================================
        System.out.println("\n=== removeTrailingSpaces 시뮬레이션 ===");

        Function<String, String> removeSpaces = example::removeTrailingSpaces;

        String input = "Line 1   \nLine 2  \nLine 3    ";
        String output = removeSpaces.apply(input);

        System.out.println("입력:");
        System.out.println("[" + input + "]");
        System.out.println("\n출력:");
        System.out.println("[" + output + "]");
    }

    // 파라미터 1개 - Function<String, String>과 호환
    public String process(String content) {
        return content.trim();
    }

    // 파라미터 1개 - Function<String, String>과 호환
    public String removeTrailingSpaces(String content) {
        if (content == null) return null;
        return java.util.Arrays.stream(content.split("\n", -1))
                .map(line -> line.replaceAll("\\s+$", ""))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    // 파라미터 2개 - BiFunction<String, String, String>과 호환
    public String concat(String a, String b) {
        return a + " " + b;
    }
}

