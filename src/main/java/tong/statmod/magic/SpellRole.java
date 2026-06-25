package tong.statmod.magic;

import tong.statmod.stats.StatType;

/**
 * Rôle d'un nœud du magic tree, qui détermine la <b>stat tertiaire</b> requise pour le
 * déverrouiller.
 *
 * <p>Chaque nœud passe trois gates de stats : ARCANE_POWER (universel), ERUDITION (universel)
 * et une tertiaire spécifique. Cette tertiaire dépend du <b>rôle gameplay</b> du sort, pas de
 * son école : un sort de mobilité est toujours gaté par AGILITY, qu'il soit Fire, Water ou
 * Eldritch.
 *
 * <p><b>Règles de classification (priorité haute → basse)</b> en cas de chevauchement :
 * <ol>
 *   <li>{@link #MOBILITY} > tout (si le sort déplace le caster, c'est sa signature)</li>
 *   <li>{@link #HEAL} > tout autre (un sort qui soigne est définitionnellement un heal)</li>
 *   <li>{@link #SUMMON} > AOE/DOT (l'invocation reste l'identité même si l'entité spam)</li>
 *   <li>{@link #BARRIER_DEFENSIVE} > {@link #DOT_ZONE} (barrière > zone DOT si elle bloque)</li>
 *   <li>{@code ELEMENTAL_DAMAGE_<X>} > {@link #AOE_BLAST} si single-target / petit cône</li>
 *   <li>{@link #AOE_BLAST} > élémental si rayon ≥ 4 blocs</li>
 *   <li>{@link #CORRUPTION_CURSE} > tout pour les écoles Eldritch/Blood</li>
 * </ol>
 *
 * <p>Les rôles {@link #TRUNK_FOUNDATION}, {@link #BRANCH_OPENER} et {@link #BRANCH_TIER}
 * désignent des nœuds <b>structurels</b> (pas des sorts) — leur tertiaire dépend de la
 * branche et est calculé par {@link MagicNodeStatRequirements}, pas exposé ici.
 */
public enum SpellRole {
    // --- Spell roles with a fixed tertiary stat ---

    /** Damage Fire direct (firebolt, fire_arrow, scorch). Tertiaire : FIRE_AFFINITY. */
    ELEMENTAL_DAMAGE_FIRE(StatType.FIRE_AFFINITY),
    /** Damage Water/Ice direct (icicle, snowball, ray_of_frost). Tertiaire : WATER_AFFINITY. */
    ELEMENTAL_DAMAGE_WATER(StatType.WATER_AFFINITY),
    /** Damage Air/Lightning direct (lightning_bolt, wind_blade). Tertiaire : AIR_AFFINITY. */
    ELEMENTAL_DAMAGE_AIR(StatType.AIR_AFFINITY),
    /** Damage Earth/Poison direct (stomp, poison_arrow, acid_orb). Tertiaire : EARTH_AFFINITY. */
    ELEMENTAL_DAMAGE_EARTH(StatType.EARTH_AFFINITY),

    /** Explosion / large zone instantanée. Tertiaire : INTIMIDATION (terreur AOE). */
    AOE_BLAST(StatType.INTIMIDATION),
    /** Zone persistante DOT > 5s (wall_of_fire, acid_rain). Tertiaire : WILLPOWER (focus soutenu). */
    DOT_ZONE(StatType.WILLPOWER),
    /** Déplacement actif (dash, step, leap). Tertiaire : AGILITY. */
    MOBILITY(StatType.AGILITY),
    /** Buff self ou allié (haste, oakskin). Tertiaire : PHYSICAL_ENDURANCE (corps prêt à porter). */
    BUFF_EMPOWERMENT(StatType.PHYSICAL_ENDURANCE),
    /** Restauration HP. Tertiaire : WILLPOWER (devotion sous pression). */
    HEAL(StatType.WILLPOWER),
    /** Invocation d'entité contrôlée. Tertiaire : TRACKING (contrôle à distance). */
    SUMMON(StatType.TRACKING),
    /** Immobilisation / bind / banish. Tertiaire : CASTING_SPEED (locker vite). */
    CONTROL_BIND(StatType.CASTING_SPEED),
    /** Mur / bulle / protection. Tertiaire : MAGIC_RESISTANCE (déflexion). */
    BARRIER_DEFENSIVE(StatType.MAGIC_RESISTANCE),
    /** Tir précis longue portée (lightning_lance, guiding_bolt). Tertiaire : PRECISION. */
    PRECISION_STRIKE(StatType.PRECISION),
    /** Téléport, portail, vide. Tertiaire : KEEN_SENSES (vision spatiale). */
    SPATIAL_VOID(StatType.KEEN_SENSES),
    /** Eldritch, drain, malédiction. Tertiaire : WILLPOWER (résister à sa propre magie). */
    CORRUPTION_CURSE(StatType.WILLPOWER),

    // --- Structural roles (no fixed tertiary — derived from branch by MagicNodeStatRequirements) ---

    /** Fondation commune (arcane_focus, mana_well, etc.) — pas de tertiaire. */
    TRUNK_FOUNDATION(null),
    /** Opener d'une école (fire/ignition, water/ice_awakening). Tertiaire par branche. */
    BRANCH_OPENER(null),
    /** Palier tier d'une école (fire/tier/ember_path). Tertiaire par (branche, tier). */
    BRANCH_TIER(null);

    private final StatType tertiaryStat;

    SpellRole(StatType tertiaryStat) {
        this.tertiaryStat = tertiaryStat;
    }

    /**
     * Stat tertiaire fixe associée au rôle, ou {@code null} si le rôle est structurel
     * (TRUNK/OPENER/TIER) — auquel cas {@link MagicNodeStatRequirements} la dérive du contexte.
     */
    public StatType tertiaryStat() {
        return tertiaryStat;
    }

    /** True si le rôle est un nœud structurel sans tertiaire fixe. */
    public boolean isStructural() {
        return tertiaryStat == null;
    }
}
