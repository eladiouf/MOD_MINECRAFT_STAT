package tong.statmod.dungeon.city;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Marqueur persistant « la cité est construite », versionné : incrémenter
 * {@link CityGenerator#CITY_VERSION} force une reconstruction au prochain démarrage
 * (utile quand la génération évolue entre deux versions du mod).
 */
public final class CitySavedData extends SavedData {

    private static final String NAME = "statmod_city";
    private static final String TAG_VERSION = "builtVersion";

    private int builtVersion = 0;

    public static CitySavedData get(ServerLevel dungeonLevel) {
        return dungeonLevel.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CitySavedData::new, CitySavedData::load, null), NAME);
    }

    private static CitySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CitySavedData data = new CitySavedData();
        data.builtVersion = tag.getInt(TAG_VERSION);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt(TAG_VERSION, builtVersion);
        return tag;
    }

    public int builtVersion() { return builtVersion; }

    public void setBuiltVersion(int version) {
        this.builtVersion = version;
        setDirty();
    }
}
