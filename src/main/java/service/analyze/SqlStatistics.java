package service.analyze;

import java.util.HashSet;
import java.util.Set;

public class SqlStatistics {
    private Set<String> tablesUsed = new HashSet<>();
    private boolean usesSelectAll;

    public void addTable(String table) {
        tablesUsed.add(table);
    }

    public Set<String> getTablesUsed() {
        return tablesUsed;
    }

    public boolean usesSelectAll() {
        return usesSelectAll;
    }

    public void setUsesSelectAll(boolean uses) {
        this.usesSelectAll = uses;
    }
}

