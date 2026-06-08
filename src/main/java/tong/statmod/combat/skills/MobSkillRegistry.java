package tong.statmod.combat.skills;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static registry of all MobSkill instances.
 * Populated at FMLCommonSetupEvent.
 */
public final class MobSkillRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger("statmod.MobSkillRegistry");
    private static final Map<ResourceLocation, MobSkill> SKILLS = new LinkedHashMap<>();

    private MobSkillRegistry() {}

    public static void register(MobSkill skill) {
        ResourceLocation id = skill.id();
        if (SKILLS.containsKey(id)) {
            LOGGER.warn("[MobSkillRegistry] Overwriting existing skill: {}", id);
        }
        SKILLS.put(id, skill);
    }

    public static MobSkill get(ResourceLocation id) {
        return SKILLS.get(id);
    }

    public static Collection<MobSkill> values() {
        return Collections.unmodifiableCollection(SKILLS.values());
    }

    /** Test only — clears the registry. */
    public static void clear() {
        SKILLS.clear();
    }
}
