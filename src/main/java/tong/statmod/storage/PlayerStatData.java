package tong.statmod.storage;

import tong.statmod.config.Config;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

public class PlayerStatData {
    public static final int STAT_COUNT = StatType.values().length;
    public static final int PERK_FAMILY_COUNT = StatFamily.values().length;
    private final int[] levels = new int[STAT_COUNT];
    private final int[] xp = new int[STAT_COUNT];
    private final int[] perkPoints = new int[PERK_FAMILY_COUNT];
    private int[] unlockedPerks = new int[0];
    private int[] freeGrantedPerks = new int[0];
    private int soulLevel;

    // Magic tree state - unified economy (Mission δ).
    /**
     * Monnaie unifiée du magic tree. Sous le nouveau modèle, c'est le seul pool dépensé pour
     * déverrouiller des nœuds. Migration depuis les anciens champs {@code arcanePoints} +
     * {@code schoolPoints} est gérée par {@link MagicStateSerializer} (capped à 100).
     */
    private int magicPoints = 5; // 5 points de départ pour débloquer les premiers nœuds
    /**
     * @deprecated Ne plus écrire — conservé en lecture pour la migration depuis les saves
     *             pré-Mission-δ. La somme est draînée vers {@link #magicPoints} au load.
     */
    @Deprecated
    private int arcanePoints;
    /**
     * @deprecated Idem {@link #arcanePoints} — pool par école drainé dans {@link #magicPoints}
     *             à la migration.
     */
    @Deprecated
    private final int[] schoolPoints = new int[tong.statmod.magic.MagicBranch.values().length];
    private final int[] schoolMasteryProgress = new int[tong.statmod.magic.MagicBranch.values().length];

    /**
     * Dernière tranche de niveau global pour laquelle des perk points ont été crédités.
     * Persisté pour éviter le bug de duplicate grant après un restart serveur (l'ancienne
     * HashMap statique {@code LevelUpHandler.lastGrantedTier} se vidait à chaque restart
     * et re-créditait au tick suivant).
     */
    private int lastPerkGrantTier;
    private String[] magicNodes = new String[0];
    private String[] learnedSpells = new String[0];
    private tong.statmod.magic.MagicRace magicRace;
    private tong.statmod.magic.MagicBranch chosenStartBranch;

    // Trial Dungeon state (Mission M6 — Phase β).
    /**
     * Plus haut étage du Trial Dungeon débloqué pour ce joueur. Défaut = 1 (l'étage 1 est
     * accessible dès qu'un portail est posé). Incrémenté par {@code DungeonBossHandler}
     * lorsqu'un boss d'étage tombe.
     */
    private int dungeonFloorReached = 1;
    /**
     * Identifiant de la dimension d'où le joueur est entré dans le donjon (typiquement
     * {@code minecraft:overworld}). Utilisé par le return beacon pour renvoyer exactement là
     * d'où le joueur vient — même s'il a entré via une dimension modée. Nullable tant que
     * le joueur n'a jamais utilisé de portail.
     */
    private String lastOverworldDimensionId;
    /**
     * Position packée ({@link net.minecraft.core.BlockPos#asLong()}) du bloc portail dans la
     * dimension d'origine. Return beacon tp exactement à cette position + 1Y (au-dessus du
     * portail). {@link #hasLastOverworldPos} indique si le champ a déjà été renseigné.
     */
    private long lastOverworldPosPacked;
    private boolean hasLastOverworldPos;
    /**
     * Points de donjon accumulés (Mission M6 — système de points, 2026-07-05). Gagnés en tuant
     * mobs/boss et en conquérant des étages ; perdus à la mort (mort punitive). Échangeables à la
     * sortie (feature à venir). Ne descend jamais sous 0.
     */
    private int dungeonPoints = 0;

    /** Iron's mana saved across sessions (persisted NBT). */
    private float storedMana = -1;

    /** Record personnel : meilleur combo de kills du donjon (persisté NBT). */
    private int dungeonBestCombo = 0;
    /** Record personnel : meilleur temps de nettoyage d'un étage de combat, en ticks (0 = aucun). */
    private int dungeonBestClearTicks = 0;

    /** Timestamp of last enchanted book right-click (server memory, not persisted). */
    public transient long lastEnchantedBookClickMs = 0;

    public int[] getLevels() { return levels.clone(); }
    public int[] getXp() { return xp.clone(); }
    public int[] getPerkPoints() { return perkPoints.clone(); }
    public int[] getUnlockedPerks() { return unlockedPerks.clone(); }
    public int[] getFreeGrantedPerks() { return freeGrantedPerks.clone(); }

    public void copyFrom(PlayerStatData source) {
        if (source == null || source == this) {
            return;
        }

        System.arraycopy(source.levels, 0, levels, 0, levels.length);
        System.arraycopy(source.xp, 0, xp, 0, xp.length);
        System.arraycopy(source.perkPoints, 0, perkPoints, 0, perkPoints.length);
        unlockedPerks = normalizePerkIds(source.unlockedPerks);
        freeGrantedPerks = retainUnlockedPerks(source.freeGrantedPerks);
        soulLevel = source.soulLevel;
        magicPoints = source.magicPoints;
        arcanePoints = source.arcanePoints;
        System.arraycopy(source.schoolPoints, 0, schoolPoints, 0, schoolPoints.length);
        System.arraycopy(source.schoolMasteryProgress, 0, schoolMasteryProgress, 0, schoolMasteryProgress.length);
        lastPerkGrantTier = source.lastPerkGrantTier;
        magicNodes = normalizeStringIds(source.magicNodes);
        learnedSpells = normalizeStringIds(source.learnedSpells);
        magicRace = source.magicRace;
        chosenStartBranch = source.chosenStartBranch;
        dungeonFloorReached = source.dungeonFloorReached;
        lastOverworldDimensionId = source.lastOverworldDimensionId;
        lastOverworldPosPacked = source.lastOverworldPosPacked;
        hasLastOverworldPos = source.hasLastOverworldPos;
        dungeonPoints = source.dungeonPoints;
        storedMana = source.storedMana;
        dungeonBestCombo = source.dungeonBestCombo;
        dungeonBestClearTicks = source.dungeonBestClearTicks;
    }

    public int getLevel(int index) { return index >= 0 && index < STAT_COUNT ? levels[index] : 0; }
    public int getXp(int index) { return index >= 0 && index < STAT_COUNT ? xp[index] : 0; }
    public int getPerkPointsForStat(int index) {
        StatType stat = StatType.byIndex(index);
        return stat != null ? getPerkPointsForFamily(stat.family()) : 0;
    }

    public int getPerkPointsForFamily(StatFamily family) {
        return family != null ? perkPoints[family.ordinal()] : 0;
    }

    public void setLevel(int index, int value) {
        if (index >= 0 && index < STAT_COUNT) {
            levels[index] = clampLevel(value);
            if (levels[index] >= maxStatLevel()) {
                xp[index] = 0;
            }
        }
    }
    public void setXp(int index, int value) {
        if (index >= 0 && index < STAT_COUNT) {
            xp[index] = levels[index] >= maxStatLevel() ? 0 : Math.max(0, value);
        }
    }
    public void setPerkPoints(int index, int value) {
        StatType stat = StatType.byIndex(index);
        if (stat != null) {
            setPerkPointsForFamily(stat.family(), value);
        }
    }

    public void setPerkPointsForFamily(StatFamily family, int value) {
        if (family != null) {
            perkPoints[family.ordinal()] = Math.max(0, value);
        }
    }

    public boolean isPerkUnlocked(int perkId) {
        for (int id : unlockedPerks) if (id == perkId) return true;
        return false;
    }

    public void addUnlockedPerk(int perkId) {
        if (perkId < 0) return;
        if (isPerkUnlocked(perkId)) return;
        int[] next = new int[unlockedPerks.length + 1];
        System.arraycopy(unlockedPerks, 0, next, 0, unlockedPerks.length);
        next[unlockedPerks.length] = perkId;
        unlockedPerks = next;
    }

    public void setUnlockedPerks(int[] ids) {
        unlockedPerks = normalizePerkIds(ids);
        freeGrantedPerks = retainUnlockedPerks(freeGrantedPerks);
    }

    public void setFreeGrantedPerks(int[] ids) {
        freeGrantedPerks = retainUnlockedPerks(ids);
    }

    public void clearUnlockedPerks() {
        unlockedPerks = new int[0];
        freeGrantedPerks = new int[0];
    }

    public void removeUnlockedPerk(int perkId) {
        if (unlockedPerks.length == 0) return;

        int count = 0;
        for (int id : unlockedPerks) {
            if (id != perkId) count++;
        }
        if (count == unlockedPerks.length) return;

        int[] next = new int[count];
        int idx = 0;
        for (int id : unlockedPerks) {
            if (id == perkId) continue;
            next[idx++] = id;
        }
        unlockedPerks = next;
        freeGrantedPerks = removeFromArray(freeGrantedPerks, perkId);
    }

    public boolean isPerkFreeGranted(int perkId) {
        for (int id : freeGrantedPerks) if (id == perkId) return true;
        return false;
    }

    public void markPerkFreeGranted(int perkId) {
        if (perkId < 0) return;
        addUnlockedPerk(perkId);
        if (isPerkFreeGranted(perkId)) return;
        int[] next = new int[freeGrantedPerks.length + 1];
        System.arraycopy(freeGrantedPerks, 0, next, 0, freeGrantedPerks.length);
        next[freeGrantedPerks.length] = perkId;
        freeGrantedPerks = next;
    }

    public void markPerkPaid(int perkId) {
        freeGrantedPerks = removeFromArray(freeGrantedPerks, perkId);
    }

    public int getSoulLevel() { return soulLevel; }

    public void setSoulLevel(int level) {
        soulLevel = Math.max(0, level);
        clampLevelsToCurrentCap();
    }

    public int getLastPerkGrantTier() { return lastPerkGrantTier; }
    public void setLastPerkGrantTier(int tier) { lastPerkGrantTier = Math.max(0, tier); }

    /**
     * @deprecated Lecture du buffer legacy (utilisée par la migration). Pour lire les points
     *             actuels, utiliser {@link #getMagicPoints()}.
     */
    @Deprecated
    public int getArcanePoints() { return arcanePoints; }
    /**
     * @deprecated Écrit dans le buffer legacy. À conserver pour deserialization des saves
     *             pré-Mission-δ. Les nouveaux callers doivent utiliser {@link #setMagicPoints(int)}.
     */
    @Deprecated
    public void setArcanePoints(int v) { arcanePoints = Math.max(0, v); }
    /**
     * Sous le modèle unifié, additionner des "points arcane" verse directement dans le pool
     * unifié. La signature est conservée pour ne pas casser les callers existants
     * ({@code CastRewardPolicy}, etc.) en attendant Mission ε qui renommera les variables.
     */
    public void addArcanePoints(int delta) { addMagicPoints(delta); }

    /** Monnaie unifiée du magic tree. */
    public int getMagicPoints() { return magicPoints; }

    public void setMagicPoints(int v) { magicPoints = Math.max(0, v); }

    public void addMagicPoints(int delta) { magicPoints = saturatingAddNonNegative(magicPoints, delta); }

    /**
     * Cap absolu pour la migration des saves pré-Mission-δ. Évite qu'un joueur dev avec un
     * stock absurde de points historiques se retrouve à débloquer tout le tree d'un coup.
     */
    public static final int MIGRATION_CAP = 100;

    /**
     * Migre les pools legacy ({@code arcanePoints} + {@code schoolPoints}) vers
     * {@link #magicPoints}. Idempotent : si {@code magicPoints} est déjà ≥ 0 et qu'aucun
     * point legacy n'est présent, ne fait rien. Appelé par {@link MagicStateSerializer} au
     * load. Retourne le nombre de points migrés (0 si rien à migrer).
     */
    public int migrateLegacyPointsToUnified() {
        if (arcanePoints == 0) {
            boolean schoolsEmpty = true;
            for (int s : schoolPoints) if (s != 0) { schoolsEmpty = false; break; }
            if (schoolsEmpty) return 0;
        }
        int legacy = arcanePoints;
        for (int s : schoolPoints) legacy += s;
        int merged = magicPoints + legacy;
        int capped = Math.min(merged, MIGRATION_CAP);
        magicPoints = capped;
        arcanePoints = 0;
        for (int i = 0; i < schoolPoints.length; i++) schoolPoints[i] = 0;
        return legacy;
    }

    /**
     * @deprecated Lecture du pool legacy par école — non-pertinent sous monnaie unifiée.
     *             Conservé pour deserialization. Pour lire les points actuels :
     *             {@link #getMagicPoints()}.
     */
    @Deprecated
    public int getSchoolPoints(tong.statmod.magic.MagicBranch b) {
        return b == null ? 0 : schoolPoints[b.ordinal()];
    }
    /** @deprecated Idem. */
    @Deprecated
    public int[] getSchoolPointsArray() { return schoolPoints.clone(); }
    /** @deprecated Écrit dans le pool legacy — utiliser {@link #setMagicPoints(int)}. */
    @Deprecated
    public void setSchoolPoints(tong.statmod.magic.MagicBranch b, int v) {
        if (b != null) schoolPoints[b.ordinal()] = Math.max(0, v);
    }
    /**
     * Sous le modèle unifié, additionner des "school points" verse dans le pool unifié.
     * Conservé pour compat callers existants ({@code SchoolProgressTracker}).
     */
    public void addSchoolPoints(tong.statmod.magic.MagicBranch b, int delta) {
        addMagicPoints(delta);
    }

    public int getSchoolMasteryProgress(tong.statmod.magic.MagicBranch b) {
        return b == null ? 0 : schoolMasteryProgress[b.ordinal()];
    }
    public void setSchoolMasteryProgress(tong.statmod.magic.MagicBranch b, int v) {
        if (b != null) schoolMasteryProgress[b.ordinal()] = Math.max(0, v);
    }
    public void addSchoolMasteryProgress(tong.statmod.magic.MagicBranch b, int delta) {
        if (b != null) schoolMasteryProgress[b.ordinal()] = saturatingAddNonNegative(schoolMasteryProgress[b.ordinal()], delta);
    }

    public String[] getMagicNodes() { return magicNodes.clone(); }
    public void setMagicNodes(String[] ids) { magicNodes = normalizeStringIds(ids); }
    public boolean hasMagicNode(String id) {
        if (!isUsableId(id)) return false;
        for (String s : magicNodes) if (s.equals(id)) return true;
        return false;
    }
    public boolean addMagicNode(String id) {
        if (!isUsableId(id) || hasMagicNode(id)) return false;
        String[] next = new String[magicNodes.length + 1];
        System.arraycopy(magicNodes, 0, next, 0, magicNodes.length);
        next[magicNodes.length] = id;
        magicNodes = next;
        return true;
    }

    public boolean removeMagicNode(String id) {
        if (!isUsableId(id) || magicNodes.length == 0 || !hasMagicNode(id)) return false;
        String[] next = new String[magicNodes.length - 1];
        int index = 0;
        for (String value : magicNodes) {
            if (id.equals(value)) continue;
            next[index++] = value;
        }
        magicNodes = next;
        return true;
    }

    public String[] getLearnedSpells() { return learnedSpells.clone(); }
    public void setLearnedSpells(String[] ids) { learnedSpells = normalizeStringIds(ids); }
    public boolean hasLearnedSpell(String id) {
        if (!isUsableId(id)) return false;
        for (String s : learnedSpells) if (s.equals(id)) return true;
        return false;
    }
    public boolean learnSpell(String id) {
        if (!isUsableId(id) || hasLearnedSpell(id)) return false;
        String[] next = new String[learnedSpells.length + 1];
        System.arraycopy(learnedSpells, 0, next, 0, learnedSpells.length);
        next[learnedSpells.length] = id;
        learnedSpells = next;
        return true;
    }

    public boolean forgetSpell(String id) {
        if (!isUsableId(id) || learnedSpells.length == 0 || !hasLearnedSpell(id)) return false;
        String[] next = new String[learnedSpells.length - 1];
        int index = 0;
        for (String value : learnedSpells) {
            if (id.equals(value)) continue;
            next[index++] = value;
        }
        learnedSpells = next;
        return true;
    }

    public tong.statmod.magic.MagicRace getMagicRace() { return magicRace; }
    public void setMagicRace(tong.statmod.magic.MagicRace race) { magicRace = race; }

    public tong.statmod.magic.MagicBranch getChosenStartBranch() { return chosenStartBranch; }
    public void setChosenStartBranch(tong.statmod.magic.MagicBranch b) { chosenStartBranch = b; }

    // Trial Dungeon — accessors (Mission M6).
    public int getDungeonFloorReached() { return dungeonFloorReached; }
    public void setDungeonFloorReached(int floor) { dungeonFloorReached = Math.max(1, floor); }
    /**
     * Débloque l'étage {@code floor} pour ce joueur sans jamais régresser. Idempotent : appeler
     * plusieurs fois avec la même valeur ne change rien ; appeler avec une valeur inférieure au
     * plus haut atteint est ignoré.
     */
    public void unlockDungeonFloor(int floor) {
        if (floor > dungeonFloorReached) dungeonFloorReached = floor;
    }

    public String getLastOverworldDimensionId() { return lastOverworldDimensionId; }
    public void setLastOverworldDimensionId(String id) { this.lastOverworldDimensionId = id; }

    public long getLastOverworldPosPacked() { return lastOverworldPosPacked; }
    public boolean hasLastOverworldPos() { return hasLastOverworldPos; }
    public void setLastOverworldPos(long packed) {
        this.lastOverworldPosPacked = packed;
        this.hasLastOverworldPos = true;
    }

    // Dungeon points (Mission M6 — système de points).
    public int getDungeonPoints() { return dungeonPoints; }
    public void setDungeonPoints(int points) { dungeonPoints = Math.max(0, points); }
    /** Ajoute des points (jamais sous 0). Retourne le nouveau total. */
    public int addDungeonPoints(int delta) {
        dungeonPoints = saturatingAddNonNegative(dungeonPoints, delta);
        return dungeonPoints;
    }

    /** Iron's mana saved across sessions. -1 means no saved value (first login). */
    public float getStoredMana() { return storedMana; }
    public void setStoredMana(float mana) { storedMana = mana; }

    /** Record : meilleur combo de kills du donjon. */
    public int getDungeonBestCombo() { return dungeonBestCombo; }
    public void setDungeonBestCombo(int combo) { dungeonBestCombo = Math.max(0, combo); }

    /** Record : meilleur temps de nettoyage d'un étage de combat (ticks, 0 = aucun). */
    public int getDungeonBestClearTicks() { return dungeonBestClearTicks; }
    public void setDungeonBestClearTicks(int ticks) { dungeonBestClearTicks = Math.max(0, ticks); }

    public int maxStatLevel() {
        int configMax = Config.getMaxStatLevel();
        return soulLevel > 0 ? Math.min(soulLevel, configMax) : configMax;
    }

    public boolean addXp(int index, int amount) {
        return addXpWithEffectiveStartLevel(index, amount, 0);
    }

    /**
     * Version de {@link #addXp(int, int)} qui calcule la courbe XP en utilisant le niveau
     * <b>effectif</b> (base + flatBonus race) plutôt que le base level seul. Évite l'incohérence
     * où un joueur Ogre +5 ENDURANCE voit son affichage à Lv 5 mais paie la courbe XP de
     * Lv 0→1 (10 XP) pour passer à l'affichage Lv 6.
     *
     * <p>Sous la nouvelle formule : un joueur affiché Lv 5 paie {@code requiredXp(5) = 360}
     * pour passer Lv 6, peu importe que ces 5 niveaux viennent du base ou du race bonus.
     */
    public boolean addXpWithEffectiveStartLevel(int index, int amount, int startLevel) {
        if (index < 0 || index >= STAT_COUNT || amount <= 0) return false;
        int cap = maxStatLevel();
        if (levels[index] >= cap) {
            levels[index] = cap;
            xp[index] = 0;
            return false;
        }
        xp[index] = saturatingAddNonNegative(xp[index], amount);
        int effective = Math.max(0, startLevel) + levels[index];
        int required = requiredXp(effective);
        boolean leveledUp = false;
        while (xp[index] >= required && levels[index] < cap) {
            levels[index]++;
            xp[index] -= required;
            effective = Math.max(0, startLevel) + levels[index];
            required = requiredXp(effective);
            leveledUp = true;
        }
        if (levels[index] >= cap) {
            levels[index] = cap;
            xp[index] = 0;
        }
        return leveledUp;
    }

    public void addLevels(int index, int amount) {
        if (index >= 0 && index < STAT_COUNT) {
            int cap = maxStatLevel();
            levels[index] = Math.min(cap, saturatingAddNonNegative(levels[index], amount));
            if (levels[index] >= cap) {
                xp[index] = 0;
            }
        }
    }

    public void addPerkPointsForStat(int index, int amount) {
        StatType stat = StatType.byIndex(index);
        if (stat != null) {
            addPerkPointsForFamily(stat.family(), amount);
        }
    }

    public void addPerkPointsForFamily(StatFamily family, int amount) {
        if (family != null) {
            int familyIndex = family.ordinal();
            perkPoints[familyIndex] = saturatingAddNonNegative(perkPoints[familyIndex], amount);
        }
    }

    /**
     * Niveau global pondéré utilisé par {@link tong.statmod.progression.LevelUpHandler} pour
     * les paliers de perk grants.
     *
     * <p><b>Refonte Mission S</b> : la formule originale faisait la moyenne arithmétique de
     * toutes les 23 stats, ce qui punissait sévèrement les builds spécialisés. Un joueur
     * full-mage à FIRE_AFFINITY Lv 10 avait un global level ≈ 0 (10 / 23) parce qu'aucune
     * stat physique / craft n'avait monté.
     *
     * <p>Nouvelle formule : moyenne des stats <b>≥ 1</b>. Les stats jamais montées sont
     * exclues du diviseur. Préserve l'ancien comportement "magic locked si pas de race" qui
     * excluait les stats magiques avant l'unlock.
     *
     * <p>Cas dégénéré : si aucune stat n'a été montée du tout, retourne 0.
     */
    public int getGlobalLevel() {
        int sum = 0;
        int divisor = 0;
        boolean magicLocked = (magicRace == null);
        for (int i = 0; i < STAT_COUNT; i++) {
            int level = levels[i];
            StatType stat = StatType.byIndex(i);
            // Tant que la race magique n'est pas choisie, les stats purement magiques
            // ne doivent pas gonfler le niveau global via commandes, migration ou data legacy.
            if (magicLocked && isMagicLockedStat(stat)) continue;
            // Skip les stats jamais montées — n'inclure que celles avec ≥ 1.
            if (level == 0) continue;
            sum += level;
            divisor++;
        }
        if (divisor == 0) return 0;
        return sum / divisor;
    }

    public static int requiredXp(int level) {
        return (level + 1) * (level + 1) * 10;
    }

    private static int saturatingAddNonNegative(int current, int delta) {
        long sum = (long) current + delta;
        if (sum <= 0L) {
            return 0;
        }
        return sum >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    private int[] retainUnlockedPerks(int[] ids) {
        if (ids == null || ids.length == 0) {
            return new int[0];
        }

        int[] next = new int[ids.length];
        int count = 0;
        for (int id : ids) {
            if (id < 0 || !isPerkUnlocked(id) || contains(next, count, id)) {
                continue;
            }
            next[count++] = id;
        }
        return copyOfLength(next, count);
    }

    private static int[] normalizePerkIds(int[] ids) {
        if (ids == null || ids.length == 0) {
            return new int[0];
        }

        int[] next = new int[ids.length];
        int count = 0;
        for (int id : ids) {
            if (id < 0 || contains(next, count, id)) {
                continue;
            }
            next[count++] = id;
        }
        return copyOfLength(next, count);
    }

    private static boolean contains(int[] source, int length, int target) {
        for (int i = 0; i < length; i++) {
            if (source[i] == target) {
                return true;
            }
        }
        return false;
    }

    private static int[] copyOfLength(int[] source, int length) {
        if (length == source.length) {
            return source;
        }
        int[] next = new int[length];
        System.arraycopy(source, 0, next, 0, length);
        return next;
    }

    private static String[] normalizeStringIds(String[] ids) {
        if (ids == null || ids.length == 0) {
            return new String[0];
        }

        String[] next = new String[ids.length];
        int count = 0;
        for (String id : ids) {
            if (!isUsableId(id) || contains(next, count, id)) {
                continue;
            }
            next[count++] = id;
        }
        return copyOfLength(next, count);
    }

    private static boolean isUsableId(String id) {
        return id != null && !id.isBlank();
    }

    private static boolean contains(String[] source, int length, String target) {
        for (int i = 0; i < length; i++) {
            if (source[i].equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static String[] copyOfLength(String[] source, int length) {
        if (length == source.length) {
            return source;
        }
        String[] next = new String[length];
        System.arraycopy(source, 0, next, 0, length);
        return next;
    }

    private static int[] removeFromArray(int[] source, int target) {
        if (source.length == 0) {
            return source;
        }

        int count = 0;
        for (int id : source) {
            if (id != target) count++;
        }
        if (count == source.length) {
            return source;
        }

        int[] next = new int[count];
        int index = 0;
        for (int id : source) {
            if (id == target) continue;
            next[index++] = id;
        }
        return next;
    }

    private int clampLevel(int value) {
        return Math.min(maxStatLevel(), Math.max(0, value));
    }

    private void clampLevelsToCurrentCap() {
        int cap = maxStatLevel();
        for (int i = 0; i < STAT_COUNT; i++) {
            if (levels[i] > cap) {
                levels[i] = cap;
            }
            if (levels[i] >= cap) {
                xp[i] = 0;
            }
        }
    }

    private static boolean isMagicLockedStat(StatType stat) {
        if (stat == null) {
            return false;
        }
        return stat.family() == StatFamily.MAGICAL_CORE
                || stat.family() == StatFamily.ELEMENTAL_SPECIALIZATION;
    }
}
