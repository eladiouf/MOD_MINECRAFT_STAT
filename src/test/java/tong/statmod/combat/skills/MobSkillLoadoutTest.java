package tong.statmod.combat.skills;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobSkillLoadoutTest {

    @Test
    void parseValidJson_extractsAllFields() {
        String json = """
            {
              "entity_type": "minecraft:zombie",
              "global_cooldown_ticks": 60,
              "skills": [
                { "id": "statmod:charge", "weight": 100 },
                { "id": "statmod:battle_cry", "weight": 30 }
              ]
            }
            """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        MobSkillLoadout loadout = MobSkillLoadout.fromJson(obj);

        assertEquals(new ResourceLocation("minecraft", "zombie"), loadout.entityType());
        assertEquals(60, loadout.globalCooldownTicks());
        assertEquals(2, loadout.entries().size());
        assertEquals(100, loadout.entries().get(0).weight());
    }

    @Test
    void parseEntryWithoutWeight_defaultsTo100() {
        String json = """
            {
              "entity_type": "minecraft:zombie",
              "skills": [ { "id": "statmod:charge" } ]
            }
            """;
        MobSkillLoadout l = MobSkillLoadout.fromJson(JsonParser.parseString(json).getAsJsonObject());
        assertEquals(100, l.entries().get(0).weight());
    }

    @Test
    void parseWithoutGlobalCooldown_defaultsTo20() {
        String json = """
            {
              "entity_type": "minecraft:zombie",
              "skills": []
            }
            """;
        MobSkillLoadout l = MobSkillLoadout.fromJson(JsonParser.parseString(json).getAsJsonObject());
        assertEquals(20, l.globalCooldownTicks());
    }
}
