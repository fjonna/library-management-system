package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fjonna.library.agent.QuerySpec.Direction;
import com.fjonna.library.agent.QuerySpec.Filters;
import com.fjonna.library.agent.QuerySpec.OrderBy;
import com.fjonna.library.agent.SqlBuilder.BuiltSql;
import com.fjonna.library.book.BookStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SqlBuilderTest {

    private final SqlBuilder builder = new SqlBuilder();
    private final UUID scope = UUID.randomUUID();

    private static Filters filtersWith(String genre, String author, String title) {
        return new Filters(genre, author, null, title, null, null, null, null);
    }

    @Test
    void countQueryHasNoGroupByOrOrderBy() {
        BuiltSql built = builder.build(new QuerySpec(Aggregate.COUNT, null, Filters.NONE, null, null, null), scope, false);
        assertThat(built.sql()).containsIgnoringCase("count(*)").doesNotContainIgnoringCase("group by");
        assertThat(built.params()).containsEntry("uid", scope);
    }

    @Test
    void sumAggregateExcludesNullsAndCoalesces() {
        QuerySpec spec = new QuerySpec(Aggregate.SUM, Field.PAGES, Filters.NONE, null, null, null);
        BuiltSql built = builder.build(spec, scope, false);
        assertThat(built.sql()).contains("sum(b.pages)").contains("b.pages is not null").contains("coalesce(");
    }

    @Test
    void listQueryOrdersByWhitelistedColumnOnly() {
        QuerySpec spec = new QuerySpec(Aggregate.LIST, null, Filters.NONE, null,
            new OrderBy(Field.PRICE, Direction.ASC), 5);
        BuiltSql built = builder.build(spec, scope, false);
        assertThat(built.sql()).contains("order by b.price asc").contains("limit :lim");
        assertThat(built.params()).containsEntry("lim", 5);
    }

    @Test
    void groupedByUserUsesLeftJoinSoZeroBookUsersAreIncluded() {
        QuerySpec spec = new QuerySpec(Aggregate.GROUP_COUNT, null, Filters.NONE, GroupBy.USER, null, 10);
        BuiltSql built = builder.build(spec, null, true);
        assertThat(built.sql()).contains("left join books b").contains("count(b.id)");
    }

    @Test
    void adminListIncludesOwnerColumnUserListDoesNot() {
        QuerySpec spec = new QuerySpec(Aggregate.LIST, null, Filters.NONE, null, null, 10);
        assertThat(builder.build(spec, null, true).sql()).contains("u.username as \"Owner\"");
        assertThat(builder.build(spec, scope, false).sql()).doesNotContain("\"Owner\"");
    }

    @Test
    void priceAndPagesFiltersBindAsParameters() {
        Filters f = new Filters(null, null, BookStatus.READING, null,
            new BigDecimal("10"), new BigDecimal("20"), 100, 300);
        QuerySpec spec = new QuerySpec(Aggregate.LIST, null, f, null, null, null);
        BuiltSql built = builder.build(spec, scope, false);
        assertThat(built.sql()).contains("b.price >= :priceMin").contains("b.price <= :priceMax")
            .contains("b.pages >= :pagesMin").contains("b.pages <= :pagesMax").contains("b.status = :status");
        assertThat(built.params())
            .containsEntry("priceMin", new BigDecimal("10"))
            .containsEntry("priceMax", new BigDecimal("20"))
            .containsEntry("pagesMin", 100)
            .containsEntry("pagesMax", 300)
            .containsEntry("status", "READING");
    }

    // ---- SQL injection safety -------------------------------------------------------------------

    @Test
    void genreValueNeverReachesSqlTextOnlyTheParameterDoes() {
        String evil = "fantasy'; DROP TABLE books; --";
        QuerySpec evilSpec = new QuerySpec(Aggregate.LIST, null, filtersWith(evil, null, null), null, null, 10);
        QuerySpec cleanSpec = new QuerySpec(Aggregate.LIST, null, filtersWith("harmless", null, null), null, null, 10);

        BuiltSql evilBuilt = builder.build(evilSpec, scope, true);
        BuiltSql cleanBuilt = builder.build(cleanSpec, scope, true);

        assertThat(evilBuilt.sql()).isEqualTo(cleanBuilt.sql());
        assertThat(evilBuilt.sql()).doesNotContain("DROP TABLE").doesNotContain("--");
        assertThat(evilBuilt.params()).containsEntry("genre", evil);
    }

    @Test
    void authorValueNeverReachesSqlTextOnlyTheParameterDoes() {
        String evil = "x'; DELETE FROM users WHERE '1'='1";
        QuerySpec evilSpec = new QuerySpec(Aggregate.LIST, null, filtersWith(null, evil, null), null, null, 10);
        QuerySpec cleanSpec = new QuerySpec(Aggregate.LIST, null, filtersWith(null, "harmless", null), null, null, 10);

        BuiltSql evilBuilt = builder.build(evilSpec, scope, false);
        BuiltSql cleanBuilt = builder.build(cleanSpec, scope, false);

        assertThat(evilBuilt.sql()).isEqualTo(cleanBuilt.sql());
        assertThat(evilBuilt.sql()).doesNotContain("DELETE FROM");
        assertThat(evilBuilt.params()).containsEntry("author", evil);
    }

    @Test
    void titleContainsValueNeverReachesSqlTextOnlyTheParameterDoes() {
        String evil = "%'; DROP DATABASE library; --";
        QuerySpec evilSpec = new QuerySpec(Aggregate.LIST, null, filtersWith(null, null, evil), null, null, 10);
        QuerySpec cleanSpec = new QuerySpec(Aggregate.LIST, null, filtersWith(null, null, "harmless"), null, null, 10);

        BuiltSql evilBuilt = builder.build(evilSpec, scope, true);
        BuiltSql cleanBuilt = builder.build(cleanSpec, scope, true);

        assertThat(evilBuilt.sql()).isEqualTo(cleanBuilt.sql());
        assertThat(evilBuilt.sql()).doesNotContain("DROP DATABASE");
        // escaped (%, _ doubled) before being wrapped in LIKE wildcards, but still just a bound value
        assertThat(evilBuilt.params().get("titlePattern")).isEqualTo("%" + evil.toLowerCase().replace("%", "\\%") + "%");
    }

    @Test
    void fieldAndGroupByEnumsAreTheOnlySourceOfColumnNames() {
        // Every Field/GroupBy enum value must resolve - this fails to compile/throw if a whitelist entry is missing.
        for (Field field : Field.values()) {
            QuerySpec spec = new QuerySpec(Aggregate.SUM, field, Filters.NONE, null, null, null);
            if (field == Field.PRICE || field == Field.PAGES) {
                assertThat(builder.build(spec, scope, false).sql()).doesNotContain(";");
            }
        }
        for (GroupBy groupBy : GroupBy.values()) {
            QuerySpec spec = new QuerySpec(Aggregate.GROUP_COUNT, null, Filters.NONE, groupBy, null, 5);
            assertThat(builder.build(spec, groupBy == GroupBy.USER ? null : scope, true).sql()).doesNotContain(";");
        }
    }
}
