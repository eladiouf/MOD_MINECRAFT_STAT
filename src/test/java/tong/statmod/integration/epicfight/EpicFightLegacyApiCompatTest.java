package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightLegacyApiCompatTest {
    @Test
    void exposesLegacyPatchedRenderersEventAddAlias() throws ClassNotFoundException {
        Class<?> alias = Class.forName("yesman.epicfight.api.client.neoevent.PatchedRenderersEvent$Add");
        assertTrue(net.neoforged.bus.api.Event.class.isAssignableFrom(alias));
    }

    @Test
    void exposesLegacyUpdatePlayerMotionAliases() throws ClassNotFoundException {
        Class<?> baseAlias = Class.forName("yesman.epicfight.api.client.neoevent.UpdatePlayerMotionEvent");
        Class<?> compositeAlias = Class.forName("yesman.epicfight.api.client.neoevent.UpdatePlayerMotionEvent$CompositeLayer");
        Class<?> baseLayerAlias = Class.forName("yesman.epicfight.api.client.neoevent.UpdatePlayerMotionEvent$BaseLayer");

        assertTrue(net.neoforged.bus.api.Event.class.isAssignableFrom(baseAlias));
        assertTrue(baseAlias.isAssignableFrom(compositeAlias));
        assertTrue(baseAlias.isAssignableFrom(baseLayerAlias));
    }

    @Test
    void exposesLegacyCoreEpicFightEventAliases() throws ClassNotFoundException {
        assertTrue(net.neoforged.bus.api.Event.class.isAssignableFrom(
                Class.forName("yesman.epicfight.api.neoevent.InitAnimatorEvent")));
        assertTrue(net.neoforged.bus.api.Event.class.isAssignableFrom(
                Class.forName("yesman.epicfight.api.neoevent.EntityPatchRegistryEvent")));
        assertTrue(net.neoforged.bus.api.Event.class.isAssignableFrom(
                Class.forName("yesman.epicfight.api.neoevent.WeaponCapabilityPresetRegistryEvent")));
    }

    @Test
    void exposesLegacyPlayerPatchEventAliases() throws ClassNotFoundException {
        assertTrue(yesman.epicfight.api.event.types.player.SkillCastEvent.class.isAssignableFrom(
                Class.forName("yesman.epicfight.api.neoevent.playerpatch.SkillCastEvent")));
        assertTrue(yesman.epicfight.api.event.types.player.SetTargetEvent.class.isAssignableFrom(
                Class.forName("yesman.epicfight.api.neoevent.playerpatch.SetTargetEvent")));
        assertTrue(yesman.epicfight.api.event.types.entity.DodgeEvent.class.isAssignableFrom(
                Class.forName("yesman.epicfight.api.neoevent.playerpatch.DodgeSuccessEvent")));

        Class<?> dealDamageAlias = Class.forName("yesman.epicfight.api.neoevent.playerpatch.DealDamageEvent");
        Class<?> dealDamagePostAlias = Class.forName("yesman.epicfight.api.neoevent.playerpatch.DealDamageEvent$Post");
        assertTrue(yesman.epicfight.api.event.types.entity.DealDamageEvent.class.isAssignableFrom(dealDamageAlias));
        assertTrue(dealDamageAlias.isAssignableFrom(dealDamagePostAlias));

        Class<?> takeDamageAlias = Class.forName("yesman.epicfight.api.neoevent.playerpatch.TakeDamageEvent");
        Class<?> takeDamagePreAlias = Class.forName("yesman.epicfight.api.neoevent.playerpatch.TakeDamageEvent$Pre");
        Class<?> takeDamagePostAlias = Class.forName("yesman.epicfight.api.neoevent.playerpatch.TakeDamageEvent$Post");
        Class<?> takeDamageIncomeAlias = Class.forName("yesman.epicfight.api.neoevent.playerpatch.TakeDamageEvent$Income");
        assertTrue(yesman.epicfight.api.event.LivingEntityPatchEvent.class.isAssignableFrom(takeDamageAlias));
        assertTrue(takeDamageAlias.isAssignableFrom(takeDamagePreAlias));
        assertTrue(takeDamageAlias.isAssignableFrom(takeDamagePostAlias));
        assertTrue(takeDamageAlias.isAssignableFrom(takeDamageIncomeAlias));
    }
}
