package yesman.epicfight.api.neoevent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.Event;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

import java.util.Map;
import java.util.function.Function;

public class EntityPatchRegistryEvent extends Event {
    private final Map<EntityType<?>, Function<Entity, EntityPatch<?>>> typeEntry;

    public EntityPatchRegistryEvent(Map<EntityType<?>, Function<Entity, EntityPatch<?>>> typeEntry) {
        this.typeEntry = typeEntry;
    }

    public Map<EntityType<?>, Function<Entity, EntityPatch<?>>> getTypeEntry() {
        return this.typeEntry;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> void registerEntityPatch(EntityType<T> type, Function<T, EntityPatch<T>> provider) {
        this.typeEntry.put(type, entity -> provider.apply((T) entity));
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> void registerEntityPatchUnsafe(EntityType<T> type,
                                                             Function<? super T, ? extends EntityPatch<? extends T>> provider) {
        this.typeEntry.put(type, entity -> (EntityPatch<?>) provider.apply((T) entity));
    }
}
