package tong.statmod.integration.ironspells.bridge;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.STATMod;
import tong.statmod.integration.tensura.TensuraSpellProfile;
import tong.statmod.integration.tensura.TensuraSpellTaxonomy;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enregistre un {@link TensuraDelegatingSpell} par compétence Tensura présente dans
 * {@link TensuraSpellTaxonomy}. Les wrappers sont injectés dans le registre Iron's Spellbooks
 * sous le namespace {@code statmod}.
 *
 * <p>Appelé une seule fois depuis le constructeur de {@link STATMod} via
 * {@link #register(IEventBus)}.
 */
public final class TensuraSpellWrapperRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(STATMod.class);

    /** DeferredRegister tied to Iron's spell registry, in the statmod namespace. */
    private static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, STATMod.MODID);

    /** Tensura skill id (canonical) → holder of the registered wrapper. */
    private static final Map<String, DeferredHolder<AbstractSpell, ? extends AbstractSpell>> HOLDERS =
            new LinkedHashMap<>();

    static {
        // Eagerly populate DeferredRegister entries at class init so that the modBus pickup is
        // immediate when STATMod calls register().
        for (TensuraSpellProfile profile : TensuraSpellTaxonomy.allProfiles()) {
            String path = TensuraWrapperIds.pathFor(profile.skillId());
            DeferredHolder<AbstractSpell, TensuraDelegatingSpell> holder =
                    SPELLS.register(path, () -> new TensuraDelegatingSpell(profile));
            HOLDERS.put(profile.skillId(), holder);
        }
    }

    private TensuraSpellWrapperRegistry() {}

    /** Wire the DeferredRegister to the mod event bus. Idempotent across calls. */
    public static void register(IEventBus modBus) {
        SPELLS.register(modBus);
        LOGGER.info("Registered {} Tensura→Iron's spell wrappers", HOLDERS.size());
    }

    /** Translate a Tensura skill id ({@code tensura:xxx}) to its wrapper id ({@code statmod:tensura_xxx}). */
    public static String wrapperIdFor(String tensuraSkillId) {
        return TensuraWrapperIds.wrapperIdFor(tensuraSkillId);
    }

    /** True if a wrapper exists in our static registry for this Tensura skill id. */
    public static boolean hasWrapperFor(String tensuraSkillId) {
        return HOLDERS.containsKey(tensuraSkillId);
    }

    /** Snapshot of all Tensura skill ids known to the wrapper registry. */
    public static Collection<String> registeredTensuraSkillIds() {
        return Collections.unmodifiableCollection(HOLDERS.keySet());
    }

    /** Number of registered wrappers. */
    public static int size() {
        return HOLDERS.size();
    }
}
