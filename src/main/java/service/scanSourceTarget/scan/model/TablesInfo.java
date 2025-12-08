package service.scanSourceTarget.scan.model;

import java.util.LinkedHashSet;
import java.util.Set;

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

    public boolean isEmpty() {
        return sources.isEmpty() && targets.isEmpty();
    }

    public void addSource(String name) {
        if (name != null && !name.isEmpty()) {
            sources.add(name);
        }
    }

    public void addTarget(String name) {
        if (name != null && !name.isEmpty()) {
            targets.add(name);
        }
    }
}

