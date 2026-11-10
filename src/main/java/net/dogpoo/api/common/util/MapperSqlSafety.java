package net.dogpoo.api.common.util;

/** Closed sets for SQL structure; values are still JDBC parameters. */
public final class MapperSqlSafety {
    private MapperSqlSafety() {}
    public static String counter(Object value) {
        if ("likeCnt".equals(value) || "dislikeCnt".equals(value)) return (String) value;
        throw new IllegalArgumentException("Unsupported counter");
    }
    public static int delta(Object value) {
        if ("+ 1".equals(value)) return 1;
        if ("- 1".equals(value)) return -1;
        throw new IllegalArgumentException("Unsupported counter adjustment");
    }
}
