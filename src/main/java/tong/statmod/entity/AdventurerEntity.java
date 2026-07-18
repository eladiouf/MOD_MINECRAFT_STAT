package tong.statmod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Aventurier humanoïde du groupe : rendu avec un <b>modèle de joueur + skin de joueur</b>
 * (voir {@code AdventurerRenderer}). Coquille volontairement « vide » côté IA : ce sont les goals
 * de rôle ({@code MageRangedGoal}, {@code TankDefendGoal}, …) attachés par {@code AdventurerPartyHelper}
 * qui pilotent le comportement. L'index de skin est synchronisé au client.
 */
public class AdventurerEntity extends Monster {

    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(AdventurerEntity.class, EntityDataSerializers.INT);

    /** Nombre de skins joueur disponibles (doit rester aligné avec AdventurerRenderer). */
    public static final int SKIN_COUNT = 5;

    public AdventurerEntity(EntityType<? extends AdventurerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void registerGoals() {
        // Seul l'anti-noyade par défaut ; le reste de l'IA vient des goals de rôle (ensureRoleAi).
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN, 0);
    }

    public int getSkin() {
        return this.entityData.get(SKIN);
    }

    public void setSkin(int index) {
        this.entityData.set(SKIN, index);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StatSkin", getSkin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSkin(tag.getInt("StatSkin"));
    }

    /** Pas de dégâts de soleil / pas de bruit de monstre : ce sont des aventuriers, pas des zombies. */
    @Override
    public boolean isSunBurnTick() {
        return false;
    }
}
