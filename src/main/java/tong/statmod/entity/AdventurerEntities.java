package tong.statmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tong.statmod.StatMod;

/** Enregistrement de l'entité aventurier (type + attributs). */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AdventurerEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StatMod.MOD_ID);

    public static final RegistryObject<EntityType<AdventurerEntity>> ADVENTURER =
            ENTITIES.register("adventurer", () -> EntityType.Builder.of(AdventurerEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("adventurer"));

    private AdventurerEntities() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ADVENTURER.get(), AdventurerEntity.createAttributes().build());
    }
}
