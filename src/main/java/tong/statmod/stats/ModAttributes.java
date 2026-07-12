package tong.statmod.stats;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ModAttributes {
    private ModAttributes() {}

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, STATMod.MODID);

    public static final Map<StatType, DeferredHolder<Attribute, Attribute>> STAT_ATTRIBUTES = new HashMap<>();

    static {
        for (StatType type : StatType.values()) {
            String name = type.name().toLowerCase(Locale.ROOT);
            STAT_ATTRIBUTES.put(type, ATTRIBUTES.register(name,
                    () -> new RangedAttribute("attribute.name.statmod." + name, 0.0, 0.0, 1000.0).setSyncable(true)));
        }
    }

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
        bus.addListener(ModAttributes::modifyEntityAttributes);
    }

    private static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        // Ajoute tous les attributs de statistiques au joueur pour qu'ils soient modifiables
        for (DeferredHolder<Attribute, Attribute> attr : STAT_ATTRIBUTES.values()) {
            event.add(EntityType.PLAYER, attr);
        }
    }
}
