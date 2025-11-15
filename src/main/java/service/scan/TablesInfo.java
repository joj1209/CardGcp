package service.scan;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Source/Target 테이블 목록 정보
 */
public class TablesInfo {
    private Set<String> sources = new LinkedHashSet<String>();
    private Set<String> targets = new LinkedHashSet<String>();

    public Set<String> getSources() {
        return sources;
    }

    public Set<String> getTargets() {
        return targets;
    }

    public boolean isEmpty() {
        return sources.isEmpty() && targets.isEmpty();
    }
}

