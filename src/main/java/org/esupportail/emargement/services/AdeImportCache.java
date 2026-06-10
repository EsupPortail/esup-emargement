package org.esupportail.emargement.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.esupportail.emargement.domain.AdeClassroomBean;
import org.esupportail.emargement.domain.AdeResourceBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache mémoire d'un run d'import ADE Campus.
 *
 * <p>Stocké via {@link ThreadLocal} pour ne pas modifier toutes les signatures des
 * méthodes du chemin d'appel. Doit être {@link #begin(String) ouvert} avant le run
 * et {@link #end() fermé} dans un bloc {@code finally} pour éviter les fuites entre
 * requêtes (avec un thread pool, un thread est réutilisé : ne pas nettoyer
 * laisserait fuiter les données d'un run précédent dans le run suivant).
 */
public final class AdeImportCache {

    private static final Logger log = LoggerFactory.getLogger(AdeImportCache.class);

    private static final ThreadLocal<AdeImportCache> CURRENT = new ThreadLocal<>();

    /** Clé : "sessionAdeId|resourceId|target". Valeur : liste des membres/codes retournés. */
    private final Map<String, List<String>> membersByKey = new HashMap<>();

    /** Clé : "sessionAdeId|resourceId|activityId". Valeur : Map d'activités. */
    private final Map<String, Map<String, AdeResourceBean>> activitiesByKey = new HashMap<>();

    /** Clé : "sessionAdeId|idItem". Valeur : liste des classrooms. */
    private final Map<String, List<AdeClassroomBean>> classroomsByKey = new HashMap<>();

    /** Clé : "sessionAdeId|id|lastImportEpoch". Valeur : booléen "a été mis à jour". */
    private final Map<String, Boolean> memberUpdatesByKey = new HashMap<>();

    private final String label;
    private final long startMs;
    private int membersHits, membersMisses;
    private int activitiesHits, activitiesMisses;
    private int classroomsHits, classroomsMisses;
    private int memberUpdatesHits, memberUpdatesMisses;

    private AdeImportCache(String label) {
        this.label = label;
        this.startMs = System.currentTimeMillis();
    }

    /**
     * Démarre un cache pour le thread courant. Si un cache existait déjà (cas d'un
     * import imbriqué ou d'une fuite), il est conservé et un warn est tracé : le
     * deuxième appel à {@link #begin(String)} ne crée pas un nouveau cache pour ne
     * pas masquer un bug de cycle de vie.
     */
    public static AdeImportCache begin(String label) {
        AdeImportCache existing = CURRENT.get();
        if (existing != null) {
            log.warn("AdeImportCache déjà actif pour ce thread (label='{}'), nouveau begin('{}') ignoré.",
                    existing.label, label);
            return existing;
        }
        AdeImportCache cache = new AdeImportCache(label);
        CURRENT.set(cache);
        log.info("PERF AdeImportCache.begin [label={}]", label);
        return cache;
    }

    /** Renvoie le cache du thread courant, ou {@code null} s'il n'y en a pas (mode non caché). */
    public static AdeImportCache current() {
        return CURRENT.get();
    }

    /**
     * Termine le cache du thread courant et trace les statistiques de hits/misses.
     * <strong>Doit être appelé dans un bloc finally</strong> de la méthode qui a
     * fait {@link #begin(String)} pour éviter les fuites de ThreadLocal.
     */
    public static void end() {
        AdeImportCache cache = CURRENT.get();
        if (cache == null) return;
        try {
            long durMs = System.currentTimeMillis() - cache.startMs;
            log.info("PERF AdeImportCache.end [label={}] [duration={} ms] " +
                            "[members hits/misses={}/{}] [activities hits/misses={}/{}] " +
                            "[classrooms hits/misses={}/{}] [memberUpdates hits/misses={}/{}]",
                    cache.label, durMs,
                    cache.membersHits, cache.membersMisses,
                    cache.activitiesHits, cache.activitiesMisses,
                    cache.classroomsHits, cache.classroomsMisses,
                    cache.memberUpdatesHits, cache.memberUpdatesMisses);
        } finally {
            CURRENT.remove();
        }
    }

    // --- Members ---

    public List<String> getMembers(String sessionAdeId, String resourceId, String target) {
        List<String> v = membersByKey.get(buildKey(sessionAdeId, resourceId, target));
        if (v != null) membersHits++; else membersMisses++;
        return v;
    }

    public void putMembers(String sessionAdeId, String resourceId, String target, List<String> value) {
        if (value == null) return;
        membersByKey.put(buildKey(sessionAdeId, resourceId, target), value);
    }

    // --- Activities ---

    public Map<String, AdeResourceBean> getActivities(String sessionAdeId, String resourceId, String activityId) {
        Map<String, AdeResourceBean> v = activitiesByKey.get(buildKey(sessionAdeId, resourceId, activityId));
        if (v != null) activitiesHits++; else activitiesMisses++;
        return v;
    }

    public void putActivities(String sessionAdeId, String resourceId, String activityId, Map<String, AdeResourceBean> value) {
        if (value == null) return;
        activitiesByKey.put(buildKey(sessionAdeId, resourceId, activityId), value);
    }

    // --- Classrooms ---

    public List<AdeClassroomBean> getClassrooms(String sessionAdeId, String idItem) {
        List<AdeClassroomBean> v = classroomsByKey.get(buildKey(sessionAdeId, idItem, ""));
        if (v != null) classroomsHits++; else classroomsMisses++;
        return v;
    }

    public void putClassrooms(String sessionAdeId, String idItem, List<AdeClassroomBean> value) {
        if (value == null) return;
        classroomsByKey.put(buildKey(sessionAdeId, idItem, ""), value);
    }

    // --- Member updates check ---

    public Boolean getMemberUpdate(String sessionAdeId, String id, long lastImportEpoch) {
        Boolean v = memberUpdatesByKey.get(buildKey(sessionAdeId, id, String.valueOf(lastImportEpoch)));
        if (v != null) memberUpdatesHits++; else memberUpdatesMisses++;
        return v;
    }

    public void putMemberUpdate(String sessionAdeId, String id, long lastImportEpoch, boolean value) {
        memberUpdatesByKey.put(buildKey(sessionAdeId, id, String.valueOf(lastImportEpoch)), value);
    }

    private static String buildKey(String a, String b, String c) {
        // Le séparateur '|' n'apparaît pas dans les ID ADE et peut donc servir de séparateur
        // sans risque de collision (les IDs ADE étant numériques, sauf en cas de listes
        // multi-IDs joints par '|' dans l'appelant — auquel cas on cache la même clé exacte).
        return (a == null ? "" : a) + "|" + (b == null ? "" : b) + "|" + (c == null ? "" : c);
    }
}