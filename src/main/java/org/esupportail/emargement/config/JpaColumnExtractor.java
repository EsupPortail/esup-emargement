package org.esupportail.emargement.config;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.EntityManagerFactory;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JpaColumnExtractor {
	
	@Autowired
	private EntityManagerFactory emf;

	private String toPhysicalColumnName(String name) {
	    // Reproduit SpringPhysicalNamingStrategy exactement :
	    // insère _ entre minuscule/chiffre et majuscule, puis lowercase
	    StringBuilder result = new StringBuilder();
	    for (int i = 0; i < name.length(); i++) {
	        char c = name.charAt(i);
	        if (i > 0 && Character.isUpperCase(c)) {
	            char prev = name.charAt(i - 1);
	            // N'insère _ que si le caractère précédent est minuscule ou chiffre
	            // → "adeVET" : V précédé de 'e' (minuscule) → "ade_VET"... 
	            // Mais Hibernate colle les séquences de majuscules
	            if (Character.isLowerCase(prev) || Character.isDigit(prev)) {
	                // Vérifie si on est au début d'un acronyme (suivant aussi majuscule)
	                boolean nextIsUpper = (i + 1 < name.length()) 
	                    && Character.isUpperCase(name.charAt(i + 1));
	                if (!nextIsUpper) {
	                    result.append('_');
	                }
	            }
	        }
	        result.append(Character.toLowerCase(c));
	    }
	    return result.toString();
	}

    /** Résout le nom de table depuis @Table ou déduit depuis le nom de classe */
    public String resolveTableName(Class<?> entityClass) {
        Table tableAnn = entityClass.getAnnotation(Table.class);
        if (tableAnn != null && !tableAnn.name().isBlank()) {
            return tableAnn.name().toLowerCase();
        }
        // Convention JPA par défaut : nom de classe → snake_case
        return toPhysicalColumnName(entityClass.getSimpleName()).toLowerCase();
    }


    public Set<String> resolveColumnNames(Class<?> entityClass) {
        Set<String> columns = new LinkedHashSet<>();
        Class<?> current = entityClass;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (isTransient(field)) continue;
                if (isMappedCollection(field)) continue;

                Column col = field.getAnnotation(Column.class);
                if (col != null && !col.name().isBlank()) {
                    columns.add(col.name().toLowerCase());
                    continue;
                }

                JoinColumn joinCol = field.getAnnotation(JoinColumn.class);
                if (joinCol != null) {
                    // Avec ou sans name explicite
                    if (!joinCol.name().isBlank()) {
                        columns.add(joinCol.name().toLowerCase());
                    } else {
                        // Convention JPA : {nomChamp}_id
                        columns.add(toPhysicalColumnName(field.getName()).toLowerCase() + "_id");
                    }
                    continue;
                }

                // @ManyToOne / @OneToOne sans @JoinColumn → convention JPA
                if (field.isAnnotationPresent(ManyToOne.class)
                        || field.isAnnotationPresent(OneToOne.class)) {
                    columns.add(toPhysicalColumnName(field.getName()).toLowerCase() + "_id");
                    continue;
                }

                JoinColumns joinCols = field.getAnnotation(JoinColumns.class);
                if (joinCols != null) {
                    for (JoinColumn jc : joinCols.value()) {
                        if (!jc.name().isBlank()) {
                            columns.add(jc.name().toLowerCase());
                        }
                    }
                    continue;
                }

                if (field.isAnnotationPresent(EmbeddedId.class)
                        || field.isAnnotationPresent(Embedded.class)) {
                    columns.addAll(resolveColumnNames(field.getType()));
                    continue;
                }

                // Champ simple sans annotation → snake_case du nom de champ
                columns.add(toPhysicalColumnName(field.getName()).toLowerCase());
            }
            current = current.getSuperclass();
        }

        return columns;
    }

    private boolean isTransient(Field field) {
        return field.isAnnotationPresent(Transient.class)
            || java.lang.reflect.Modifier.isTransient(field.getModifiers());
    }

    private boolean isMappedCollection(Field field) {
        // @OneToMany / @ManyToMany n'ont pas de colonne dans la table principale
        return field.isAnnotationPresent(OneToMany.class)
            || field.isAnnotationPresent(ManyToMany.class);
    }

    public Set<String> resolveJoinTableNames(Class<?> entityClass) {
        Set<String> joinTables = new LinkedHashSet<>();
        Class<?> current = entityClass;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                JoinTable jt = field.getAnnotation(JoinTable.class);
                if (jt != null && !jt.name().isBlank()) {
                    joinTables.add(jt.name().toLowerCase());
                }
            }
            current = current.getSuperclass();
        }

        return joinTables;
    }
}