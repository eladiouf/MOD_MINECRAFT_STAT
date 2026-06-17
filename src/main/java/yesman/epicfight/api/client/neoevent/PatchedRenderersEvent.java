package yesman.epicfight.api.client.neoevent;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.bus.api.Event;
import net.minecraft.world.entity.EntityType;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;

import java.util.Map;
import java.util.function.Function;

public abstract class PatchedRenderersEvent extends Event {
    public static final class Add extends PatchedRenderersEvent {
        private final Map<EntityType<?>, Function<EntityType<?>, PatchedEntityRenderer>> typeEntry;
        private final EntityRendererProvider.Context context;

        public Add(Map<EntityType<?>, Function<EntityType<?>, PatchedEntityRenderer>> typeEntry,
                   EntityRendererProvider.Context context) {
            this.typeEntry = typeEntry;
            this.context = context;
        }

        public void addPatchedEntityRenderer(EntityType<?> type,
                                             Function<EntityType<?>, PatchedEntityRenderer> rendererFactory) {
            this.typeEntry.put(type, rendererFactory);
        }

        public EntityRendererProvider.Context getContext() {
            return this.context;
        }
    }
}
