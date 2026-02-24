package uss.code.global.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;

import java.util.Locale;

public class P6SpySqlFormatter implements MessageFormattingStrategy {
    @Override
    public String formatMessage(
            final int connectionId,
            final String now,
            final long elapsed,
            final String category,
            final String prepared,
            final String sql,
            final String url
    ) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        final String formattedSql = formatSql(category, sql);

        return String.format("\n[P6Spy - %dms]\n%s\n", elapsed, formattedSql);
    }

    private String formatSql(
            final String category,
            final String sql
    ) {
        if (Category.STATEMENT.getName().equals(category)) {
            final String lowerSql = sql.trim().toLowerCase(Locale.ROOT);

            if (lowerSql.startsWith("create") ||
                    lowerSql.startsWith("alter") ||
                    lowerSql.startsWith("comment")) {
                return FormatStyle.DDL.getFormatter().format(sql);
            }
            return FormatStyle.BASIC.getFormatter().format(sql);
        }
        return sql;
    }
}
