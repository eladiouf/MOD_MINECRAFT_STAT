package tong.statmod.integration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicParcoolCompatStateTest {

    @Test
    @SuppressWarnings("unchecked")
    void cleanupPlayerCooldowns_removesOnlyMatchingPlayerKeys() throws Exception {
        UUID target = UUID.randomUUID();
        String targetKey = target + ":xp:WallJump";
        String targetResourceKey = target + ":res";
        String foreignKey = UUID.randomUUID() + ":xp:WallJump";

        Field loadedField = EpicParcoolCompat.class.getDeclaredField("loaded");
        loadedField.setAccessible(true);
        boolean previousLoaded = loadedField.getBoolean(null);
        loadedField.setBoolean(null, true);

        try {
            Class<?> hookClass = Class.forName("tong.statmod.integration.EpicParcoolCompat$ParCoolHookRegistry");
            Field xpCooldownsField = hookClass.getDeclaredField("xpCooldowns");
            Field resourceCooldownsField = hookClass.getDeclaredField("resourceCooldowns");
            xpCooldownsField.setAccessible(true);
            resourceCooldownsField.setAccessible(true);

            Map<String, Integer> xpCooldowns = (Map<String, Integer>) xpCooldownsField.get(null);
            Map<String, Integer> resourceCooldowns = (Map<String, Integer>) resourceCooldownsField.get(null);
            xpCooldowns.clear();
            resourceCooldowns.clear();
            xpCooldowns.put(targetKey, 10);
            xpCooldowns.put(foreignKey, 20);
            resourceCooldowns.put(targetResourceKey, 30);

            Method cleanupMethod = EpicParcoolCompat.class.getDeclaredMethod("cleanupPlayerCooldowns", UUID.class);
            cleanupMethod.setAccessible(true);
            cleanupMethod.invoke(null, target);

            assertFalse(xpCooldowns.containsKey(targetKey));
            assertFalse(resourceCooldowns.containsKey(targetResourceKey));
            assertTrue(xpCooldowns.containsKey(foreignKey));
        } finally {
            loadedField.setBoolean(null, previousLoaded);
        }
    }
}
