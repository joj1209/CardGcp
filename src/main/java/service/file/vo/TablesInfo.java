package service.file.vo;

import java.util.*;

/**
 * Source/Target 테이블 목록을 보관하는 VO.
 */
public class TablesInfo {
    private final Set<String> sources = new LinkedHashSet<>();
    private final Set<String> targets = new LinkedHashSet<>();

    public Set<String> getSources() {
        return sources;
    }

    public Set<String> getTargets() {
        return targets;
    }

    /**
     * 정렬된 소스 테이블 목록을 반환합니다.
     *
     * @return 알파벳순으로 정렬된 소스 테이블 Set
     */
    public Set<String> getSortedSources() {
        return new TreeSet<>(sources);
    }

    /**
     * 정렬된 타겟 테이블 목록을 반환합니다.
     *
     * @return 알파벳순으로 정렬된 타겟 테이블 Set
     */
    public Set<String> getSortedTargets() {
        return new TreeSet<>(targets);
    }

    public boolean isEmpty() {
        return sources.isEmpty() && targets.isEmpty();
    }

    public void addSource(String name) {
        if (name != null && !name.isEmpty()) sources.add(name);
    }

    public void addTarget(String name) {
        if (name != null && !name.isEmpty()) targets.add(name);
    }
}

