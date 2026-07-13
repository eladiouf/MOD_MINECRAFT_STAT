package tong.statmod.stats;

public final class StatCombatScaling {
    private StatCombatScaling() {}

    public enum IncomingDamageRole {
        PHYSICAL,
        MAGIC,
        STATUS,
        ENVIRONMENT
    }

    /** Niveau max d'une stat (aligné sur {@code Config.maxStatLevel}). Sert à normaliser la courbe. */
    private static final float MAX_LEVEL = 100f;

    /**
     * Multiplicateur de dégâts de mêlée du joueur selon la stat de son arme.
     *
     * <p>Refonte 2026-07-05 (« les armes tapent trop faible ») : la courbe était bien trop plate
     * (+0,5 %/niveau → ×1,5 max au niveau 100, ridicule face aux mobs L2/boss à centaines de PV).
     * Nouvelle courbe pilotée par la config :
     * <pre>{@code mult = base + scale * (level/100)^1.5}</pre>
     * <ul>
     *   <li>{@code base} = plancher au niveau 0 (défaut 1,5 → toutes les armes tapent déjà correct).</li>
     *   <li>{@code scale} = amplitude ajoutée au niveau max (défaut 8,5 → stat principale maxée = ×10).</li>
     *   <li>Exposant 1,5 : progression un peu accélérante (l'investissement se sent en profondeur).</li>
     * </ul>
     * Les stats secondaires (Rapidité/Arcane) reçoivent 60 % de l'amplitude (×6,6 au niveau 100).
     */
    public static float weaponDamageMultiplier(StatType weaponStat,
                                               int bruteForce,
                                               int bladeTechnique,
                                               int precision,
                                               int rapidite,
                                               int arcanePower,
                                               float base,
                                               float scale) {
        if (weaponStat == null) {
            return base;
        }
        return switch (weaponStat) {
            case BRUTE_FORCE -> curve(bruteForce, base, scale, 1.0f);
            case BLADE_TECHNIQUE -> curve(bladeTechnique, base, scale, 1.0f);
            case PRECISION -> curve(precision, base, scale, 1.0f);
            case RAPIDITE -> curve(rapidite, base, scale, 0.6f);
            case ARCANE_POWER -> curve(arcanePower, base, scale, 0.6f);
            default -> base;
        };
    }

    /** Courbe {@code base + scale*weight*(lvl/max)^1.5}. */
    private static float curve(int level, float base, float scale, float weight) {
        float t = Math.max(0, Math.min(1f, level / MAX_LEVEL));
        return base + scale * weight * (float) Math.pow(t, 1.5);
    }

    public static float intimidationDamageMultiplier(int intimidation, boolean markedTarget) {
        // Balance 30j : +0.5% dmg par level sur cible marquée au lieu de 0.3%.
        return markedTarget ? 1.0f + Math.max(0, intimidation) * 0.005f : 1.0f;
    }

    public static float physicalDamageTakenMultiplier(int physicalResistance) {
        return 1.0f - Math.min(0.65f, Math.max(0, physicalResistance) * 0.0065f);
    }

    public static float magicDamageTakenMultiplier(int magicResistance) {
        return 1.0f - Math.min(0.65f, Math.max(0, magicResistance) * 0.0065f);
    }

    public static float enduranceDamageTakenMultiplier(int physicalEndurance) {
        return 1.0f - Math.min(0.35f, Math.max(0, physicalEndurance) * 0.0035f);
    }

    public static float statusDamageTakenMultiplier(int willpower) {
        return 1.0f - Math.min(0.45f, Math.max(0, willpower) * 0.0045f);
    }

    public static float incomingDamageMultiplier(IncomingDamageRole role,
                                                 int physicalResistance,
                                                 int magicResistance,
                                                 int physicalEndurance,
                                                 int willpower) {
        if (role == null) {
            return 1.0f;
        }
        return switch (role) {
            case PHYSICAL -> physicalDamageTakenMultiplier(physicalResistance)
                    * enduranceDamageTakenMultiplier(physicalEndurance);
            case MAGIC -> magicDamageTakenMultiplier(magicResistance);
            case STATUS -> statusDamageTakenMultiplier(willpower);
            case ENVIRONMENT -> 1.0f;
        };
    }

    public static IncomingDamageRole damageRole(boolean magicDamage, boolean indirectMagicDamage, String messageId) {
        String id = messageId == null ? "" : messageId.toLowerCase(java.util.Locale.ROOT);
        if (isStatusDamageId(id)) {
            return IncomingDamageRole.STATUS;
        }
        if (magicDamage || indirectMagicDamage) {
            return IncomingDamageRole.MAGIC;
        }
        if (isEnvironmentDamageId(id)) {
            return IncomingDamageRole.ENVIRONMENT;
        }
        return IncomingDamageRole.PHYSICAL;
    }

    public static int negativeEffectDurationTicks(int originalDurationTicks, int willpower, boolean ironWillPerkUnlocked) {
        if (originalDurationTicks <= 0) {
            return originalDurationTicks;
        }
        float reduction = Math.min(0.45f, Math.max(0, willpower) * 0.0045f);
        if (ironWillPerkUnlocked) {
            reduction += 0.10f;
        }
        reduction = Math.min(0.65f, reduction);
        return Math.max(1, Math.round(originalDurationTicks * (1.0f - reduction)));
    }

    private static boolean isStatusDamageId(String id) {
        return id.contains("poison")
                || id.contains("wither")
                || id.contains("freeze")
                || id.contains("thorns");
    }

    private static boolean isEnvironmentDamageId(String id) {
        return id.contains("fall")
                || id.contains("starve")
                || id.contains("drown")
                || id.contains("inwall")
                || id.contains("in_wall")
                || id.contains("outofworld")
                || id.contains("out_of_world")
                || id.contains("lava")
                || id.contains("onfire")
                || id.contains("on_fire")
                || id.contains("hotfloor")
                || id.contains("hot_floor")
                || id.contains("cactus")
                || id.contains("sweetberrybush")
                || id.contains("sweet_berry_bush");
    }
}
