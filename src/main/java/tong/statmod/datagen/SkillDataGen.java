package tong.statmod.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;
import tong.statmod.skills.SkillRegistry;
import tong.statmod.skills.StatModSkillCategories;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.skill.SkillSlots;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SkillDataGen {

    public static void generateAll() {
        Path outputDir = Paths.get("src/main/resources/data/statmod/epicfight/skill");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            STATMod.LOGGER.error("Failed to create skill data directory", e);
            return;
        }

        for (SkillEntry entry : collectSkills()) {
            JsonObject root = new JsonObject();
            root.addProperty("resource", entry.resource);
            if (entry.duration > 0) root.addProperty("duration", entry.duration);
            if (entry.stack > 1) root.addProperty("stack", entry.stack);
            if (!entry.animations.isEmpty()) {
                JsonArray anims = new JsonArray();
                for (String anim : entry.animations) anims.add(anim);
                root.add("animations", anims);
            }
            if (!entry.properties.isEmpty()) {
                JsonObject props = new JsonObject();
                for (var e : entry.properties.entrySet()) props.addProperty(e.getKey(), e.getValue());
                root.add("properties", props);
            }

            Path filePath = outputDir.resolve(entry.name + ".json");
            try (FileWriter fw = new FileWriter(filePath.toFile())) {
                var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                fw.write(gson.toJson(root));
            } catch (IOException e) {
                STATMod.LOGGER.error("Failed to write skill data: {}", entry.name, e);
            }
        }

        STATMod.LOGGER.info("Generated {} skill JSON files", collectSkills().size());
    }

    private static List<SkillEntry> collectSkills() {
        List<SkillEntry> entries = new ArrayList<>();

        addWeaponPassive(entries);
        addStatPassive(entries);
        addWeaponInnate(entries);
        addMover(entries);
        addGuard(entries);
        addIdentity(entries);
        addNonCombat(entries);

        return entries;
    }

    private static void addWeaponPassive(List<SkillEntry> entries) {
        String[] weapons = {"sword", "axe", "dagger", "greatsword", "longsword",
            "spear", "tachi", "uchigatana", "fist", "bow", "pickaxe", "hoe"};
        for (String w : weapons) {
            entries.add(new SkillEntry(w + "_mastery", "skill/statmod:" + w + "_mastery", 0));
        }
    }

    private static void addStatPassive(List<SkillEntry> entries) {
        String[][] passives = {
            {"brute", "power", "rage", "fury"},
            {"blade", "finesse", "mastery", "perfection"},
            {"rapid", "surge", "blitz", "lightning"},
            {"agility", "footwork", "evasion", "phantom"},
            {"resist", "iron", "steel", "diamond"},
            {"endurance", "vitality", "toughness", "unbreakable"},
            {"precision", "focus", "accuracy", "deadeye"},
        };
        for (String[] group : passives) {
            for (String name : java.util.Arrays.copyOfRange(group, 1, group.length)) {
                entries.add(new SkillEntry(group[0] + "_" + name, "skill/statmod:" + group[0] + "_" + name, 0));
            }
        }
    }

    private static void addWeaponInnate(List<SkillEntry> entries) {
        entries.add(new SkillEntry("heavy_strike", "skill/statmod:heavy_strike", 20));
        entries.add(new SkillEntry("blade_dance", "skill/statmod:blade_dance", 25));
        entries.add(new SkillEntry("blitz_assault", "skill/statmod:blitz_assault", 30));
        entries.add(new SkillEntry("shadow_step", "skill/statmod:shadow_step", 20));
        entries.add(new SkillEntry("stone_skin", "skill/statmod:stone_skin", 30));
        entries.add(new SkillEntry("endurance_surge", "skill/statmod:endurance_surge", 35));
        entries.add(new SkillEntry("precision_shot", "skill/statmod:precision_shot", 25));
    }

    private static void addMover(List<SkillEntry> entries) {
        entries.add(new SkillEntry("quick_step", "skill/statmod:quick_step", 10));
        entries.add(new SkillEntry("shadow_leap", "skill/statmod:shadow_leap", 15));
        entries.add(new SkillEntry("wind_dash", "skill/statmod:wind_dash", 15));
    }

    private static void addGuard(List<SkillEntry> entries) {
        entries.add(new SkillEntry("parry", "skill/statmod:parry", 10));
        entries.add(new SkillEntry("guard_fortress", "skill/statmod:guard_fortress", 20));
        entries.add(new SkillEntry("guard_iron_wall", "skill/statmod:guard_iron_wall", 25));
    }

    private static void addIdentity(List<SkillEntry> entries) {
        entries.add(new SkillEntry("berserker_rage", "skill/statmod:berserker_rage", 60));
        entries.add(new SkillEntry("blade_god", "skill/statmod:blade_god", 90));
        entries.add(new SkillEntry("shadow_dancer", "skill/statmod:shadow_dancer", 120));
    }

    private static void addNonCombat(List<SkillEntry> entries) {
        String[] skills = {"arcane_bolt", "water_heal", "earth_shield", "fireball", "air_dash",
            "magic_resist", "fast_cast", "mana_regen", "study", "track", "sense_danger",
            "master_forge", "feast", "potion_boost", "meditate", "intimidate", "willpower_aura"};
        int[] durations = {10, 15, 15, 12, 10, 20, 12, 12, 10, 12, 8, 10, 10, 10, 20, 12, 15};
        for (int i = 0; i < skills.length; i++) {
            entries.add(new SkillEntry(skills[i], "skill/statmod:" + skills[i], durations[i]));
        }
    }

    private static class SkillEntry {
        final String name;
        final String resource;
        final int duration;
        final int stack;
        final List<String> animations;
        final Map<String, String> properties;

        SkillEntry(String name, String resource, int duration) {
            this.name = name;
            this.resource = resource;
            this.duration = duration;
            this.stack = 1;
            this.animations = new ArrayList<>();
            this.properties = new LinkedHashMap<>();
        }
    }
}
