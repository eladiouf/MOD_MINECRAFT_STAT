package tong.statmod.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StatModMixinPlugin implements IMixinConfigPlugin {
    private static final Map<String, String> OPTIONAL_MIXINS = Map.ofEntries(
            Map.entry("OvergearedSmithingScreenMixin", "overgeared"),
            Map.entry("OvergearedSmithingMixin", "overgeared"),
            Map.entry("OvergearedSmithingMenuMixin", "overgeared"),
            Map.entry("OvergearedForgingRecipeMixin", "overgeared"),
            Map.entry("OvergearedAlloySmelterMixin", "overgeared"),
            Map.entry("EpicFightSkillBookScreenMixin", "epicfight"),
            Map.entry("EpicFightPlayerScaleMixin", "epicfight"),
            Map.entry("ItemKeywordReloadListenerMixin", "epicfight"),
            Map.entry("EpicFightSkillBuilderMixin", "epicfight"),
            Map.entry("EpicFightPlayerSkillsMixin", "epicfight"),
            Map.entry("EpicFightNullAnimationMixin", "epicfight"),
            Map.entry("IronInscriptionTableScreenMixin", "irons_spellbooks"),
            Map.entry("IronInscriptionTableMenuMixin", "irons_spellbooks"),
            Map.entry("IronSpellIconResolverMixin", "irons_spellbooks"),
            Map.entry("IronSpellEquipmentChangeMixin", "irons_spellbooks"),
            Map.entry("SpellSelectionManagerMixin", "irons_spellbooks"),
            Map.entry("IronLearnedSpellCastSourceMixin", "irons_spellbooks"),
            Map.entry("PuffishSkillsScreenMixin", "puffish_skills"),
            Map.entry("ReincarnationMenuRaceFilterMixin", "tensura"),
            Map.entry("ShopPageMixin", "sdmshop"),
            Map.entry("ShopPageModernMixin", "sdmshop"),
            Map.entry("ShopTabPanelMixin", "sdmshop"),
            Map.entry("IndestructibleServerMixin", "indestructible")
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String requiredModid = OPTIONAL_MIXINS.get(simpleName(mixinClassName));
        return requiredModid == null || isModPresent(requiredModid);
    }

    /**
     * Vérifie la présence d'un mod <b>pendant la phase mixin</b>. On NE PEUT PAS utiliser
     * {@code ModList.get()} ici : les mixins s'appliquent avant que {@code ModList} soit construit,
     * donc {@code ModList.get()} renvoie {@code null} → NPE au lancement (le jeu ne démarre pas).
     * {@link LoadingModList} est, lui, disponible tôt.
     */
    private static boolean isModPresent(String modid) {
        LoadingModList list = LoadingModList.get();
        return list != null && list.getModFileById(modid) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static String simpleName(String mixinClassName) {
        int separator = mixinClassName.lastIndexOf('.');
        return separator >= 0 ? mixinClassName.substring(separator + 1) : mixinClassName;
    }
}
