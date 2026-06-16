package tong.statmod.integration.parcool;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcoolAttributeHandlerTest {
    @Test
    void usesSeparateCachesForEachTrackedParcoolAttribute() throws IllegalAccessException {
        Map<String, Field> mapFields = Arrays.stream(ParcoolAttributeHandler.class.getDeclaredFields())
                .filter(field -> Map.class.isAssignableFrom(field.getType()))
                .peek(field -> field.setAccessible(true))
                .collect(Collectors.toMap(Field::getName, field -> field));

        assertTrue(mapFields.containsKey("lastMaxStamina"));
        assertTrue(mapFields.containsKey("lastStaminaRecovery"));
        assertTrue(mapFields.containsKey("lastAgility"));

        Object maxStaminaCache = mapFields.get("lastMaxStamina").get(null);
        Object staminaRecoveryCache = mapFields.get("lastStaminaRecovery").get(null);

        assertNotSame(maxStaminaCache, staminaRecoveryCache);
    }
}
