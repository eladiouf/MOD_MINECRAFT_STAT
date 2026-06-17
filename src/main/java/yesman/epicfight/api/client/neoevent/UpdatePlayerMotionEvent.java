package yesman.epicfight.api.client.neoevent;

import net.neoforged.bus.api.Event;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

public abstract class UpdatePlayerMotionEvent extends Event {
    private final AbstractClientPlayerPatch<?> playerPatch;
    private LivingMotion motion;

    protected UpdatePlayerMotionEvent(AbstractClientPlayerPatch<?> playerPatch, LivingMotion motion) {
        this.playerPatch = playerPatch;
        this.motion = motion;
    }

    public void setMotion(LivingMotion motion) {
        this.motion = motion;
    }

    public LivingMotion getMotion() {
        return this.motion;
    }

    public AbstractClientPlayerPatch<?> getPlayerPatch() {
        return this.playerPatch;
    }

    public static final class BaseLayer extends UpdatePlayerMotionEvent {
        private final boolean inaction;

        public BaseLayer(AbstractClientPlayerPatch<?> playerPatch, LivingMotion motion, boolean inaction) {
            super(playerPatch, motion);
            this.inaction = inaction;
        }

        public boolean inaction() {
            return this.inaction;
        }
    }

    public static final class CompositeLayer extends UpdatePlayerMotionEvent {
        public CompositeLayer(AbstractClientPlayerPatch<?> playerPatch, LivingMotion motion) {
            super(playerPatch, motion);
        }
    }
}
