package com.sortease.sort;

/** 整理模式。序号即客户端配置 sortMode 的取值，两端同一 jar 保证一致。 */
public enum SortMode {
    MERGE("sort.mode.merge"),
    NAME("sort.mode.name"),
    ID("sort.mode.id"),
    MOD("sort.mode.mod"),
    COUNT("sort.mode.count");

    public final String langKey;

    SortMode(String langKey) {
        this.langKey = langKey;
    }

    public static SortMode byOrdinal(int ordinal) {
        SortMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : MERGE;
    }
}
