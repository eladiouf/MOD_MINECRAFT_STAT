package tong.statmod.weapon;

import yesman.epicfight.world.capabilities.item.CapabilityItem;

public enum WeaponType {
    SWORD(CapabilityItem.WeaponCategories.SWORD),
    GREATSWORD(CapabilityItem.WeaponCategories.GREATSWORD),
    UCHIGATANA(CapabilityItem.WeaponCategories.UCHIGATANA),
    SPEAR(CapabilityItem.WeaponCategories.SPEAR),
    DAGGER(CapabilityItem.WeaponCategories.DAGGER),
    AXE(CapabilityItem.WeaponCategories.AXE),
    FIST(CapabilityItem.WeaponCategories.FIST),
    BOW(CapabilityItem.WeaponCategories.BOW),
    CROSSBOW(CapabilityItem.WeaponCategories.CROSSBOW),
    TRIDENT(CapabilityItem.WeaponCategories.TRIDENT),
    TACHI(CapabilityItem.WeaponCategories.TACHI),
    LONGSWORD(CapabilityItem.WeaponCategories.LONGSWORD),
    SHIELD(CapabilityItem.WeaponCategories.SHIELD);

    public final CapabilityItem.WeaponCategories epicFightCategory;

    WeaponType(CapabilityItem.WeaponCategories epicFightCategory) {
        this.epicFightCategory = epicFightCategory;
    }

    public static WeaponType fromEpicFightCategory(CapabilityItem.WeaponCategories category) {
        for (WeaponType type : values()) {
            if (type.epicFightCategory == category) return type;
        }
        return null;
    }
}
