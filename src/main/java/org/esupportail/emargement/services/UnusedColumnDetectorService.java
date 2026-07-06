package org.esupportail.emargement.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.sql.DataSource;

import org.esupportail.emargement.config.JpaColumnExtractor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UnusedColumnDetectorService {

    private final EntityManagerFactory emf;
    private final JdbcTemplate jdbcTemplate;
    private final JpaColumnExtractor columnExtractor;

    public UnusedColumnDetectorService(
            EntityManagerFactory emf,
            @Qualifier("localDb") DataSource localDataSource,  // ← force le bon DS
            JpaColumnExtractor columnExtractor) {
        this.emf = emf;
        this.jdbcTemplate = new JdbcTemplate(localDataSource); // ← construit depuis le bon DS
        this.columnExtractor = columnExtractor;
    }
    /**
     * Retourne les colonnes présentes en base mais absentes des @Entity JPA.
     * Clé = nom de table, valeur = liste des colonnes orphelines.
     */
    public Map<String, List<String>> findUnusedColumns() {
        Map<String, Set<String>> jpaColumns  = extractJpaColumns();
        Set<String>              joinTables  = extractJoinTableNames(); // ← nouveau
        Map<String, Set<String>> dbColumns   = fetchDatabaseColumns();

        Map<String, List<String>> unused = new LinkedHashMap<>();

        dbColumns.forEach((table, dbCols) -> {
            // Ignorer les tables de jointure @ManyToMany
        	if (joinTables.stream()
        	        .anyMatch(jt -> normalizeColumnName(jt)
        	            .equals(normalizeColumnName(table)))) {
        	    return;
        	}

            Set<String> mapped = jpaColumns.getOrDefault(table, Collections.emptySet());

            List<String> orphans = dbCols.stream()
            	    .filter(col -> mapped.stream()
            	        .noneMatch(jpa -> normalizeColumnName(jpa)
            	            .equals(normalizeColumnName(col))))
            	    .sorted()
            	    .collect(Collectors.toList());

            if (!orphans.isEmpty()) {
                unused.put(table, orphans);
            }
        });

        return unused;
    }
    
    private String normalizeColumnName(String name) {
        return name
            .replaceAll("([a-z])([A-Z])", "$1_$2")
            .replace("_", "")
            .toLowerCase();
    }

    private Set<String> extractJoinTableNames() {
        Set<String> joinTables = new HashSet<>();
        for (EntityType<?> entity : emf.getMetamodel().getEntities()) {
            joinTables.addAll(columnExtractor.resolveJoinTableNames(entity.getJavaType()));
        }
        return joinTables;
    }

    // ------------------------------------------------------------------ //
    //  JPA side : lire le metamodel + annotations @Column / @JoinColumn   //
    // ------------------------------------------------------------------ //

    private Map<String, Set<String>> extractJpaColumns() {
        Map<String, Set<String>> result = new HashMap<>();

        for (EntityType<?> entity : emf.getMetamodel().getEntities()) {
            String tableName = columnExtractor.resolveTableName(entity.getJavaType());
            Set<String> cols = columnExtractor.resolveColumnNames(entity.getJavaType());
            result.merge(tableName, cols, (a, b) -> { a.addAll(b); return a; });
        }

        return result;
    }

    // ------------------------------------------------------------------ //
    //  DB side : information_schema                                        //
    // ------------------------------------------------------------------ //

    private Map<String, Set<String>> fetchDatabaseColumns() {
        String sql = "SELECT table_name, column_name " +
              "FROM information_schema.columns " +
             "WHERE table_schema = current_schema() " +
             "ORDER BY table_name, column_name";

        Map<String, Set<String>> result = new LinkedHashMap<>();

        jdbcTemplate.query(sql, rs -> {
            String table  = rs.getString("table_name");
            String column = rs.getString("column_name");
            result.computeIfAbsent(table, k -> new LinkedHashSet<>()).add(column);
        });

        return result;
    }
}