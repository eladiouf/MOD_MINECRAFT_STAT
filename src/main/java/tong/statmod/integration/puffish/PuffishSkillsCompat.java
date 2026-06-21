package tong.statmod.integration.puffish;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.STATMod;
import tong.statmod.magic.MagicUnlockFeedbackMessageFactory;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicTreeCatalog;
import tong.statmod.magic.MagicTreeProgressionService;
import tong.statmod.network.SyncHelper;
import tong.statmod.network.PerkFeedbackPayload;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkFeedbackMessageFactory;
import tong.statmod.perks.PerkManager;
import tong.statmod.sound.SoundHelper;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PuffishSkillsCompat {
    private static final String SKILLS_API_CLASS = "net.puffish.skillsmod.api.SkillsAPI";
    private static boolean loaded;
    private static final Set<UUID> SYNC_GUARD = ConcurrentHashMap.newKeySet();

    private PuffishSkillsCompat() {}

    public static void init() {
        loaded = ModList.get().isLoaded("puffish_skills");
        if (!loaded) {
            STATMod.LOGGER.info("Puffish Skills not detected, skipping PuffishSkillsCompat");
            return;
        }

        try {
            registerEvent("net.puffish.skillsmod.api.Events$SkillUnlock", "registerSkillUnlockEvent",
                    (player, categoryId, skillId) -> {
                        if (isSyncing(player)) return;
                        PlayerStatData data = player.getData(ModAttachments.STATS);

                        if (categoryId != null && categoryId.startsWith("statmod:statmod_magic_")) {
                            String nodeId = PuffishMagicCategoryIds.fromSkillId(skillId);
                            MagicNode node = MagicTreeCatalog.byId(nodeId);
                            if (node == null) return;
                            var result = MagicTreeProgressionService.tryUnlock(data, node, player);
                            if (result.success()) {
                                SoundHelper.playPerkUnlock(player);
                            } else {
                                MagicUnlockFeedbackMessageFactory.MagicUnlockFeedbackMessage feedback =
                                        MagicUnlockFeedbackMessageFactory.forFailure(node, result.failure());
                                PacketDistributor.sendToPlayer(player, new PerkFeedbackPayload(
                                        feedback.title().getString(),
                                        feedback.message().getString()));
                            }
                            SyncHelper.syncMagic(player);
                            return;
                        }

                        Perk perk = PuffishPerkIds.resolve(categoryId, skillId);
                        PerkManager manager = new PerkManager(data);
                        PerkManager.UnlockFailure failure = manager.getUnlockFailure(perk, player);
                        if (failure == null && manager.unlock(perk, player)) {
                            SoundHelper.playPerkUnlock(player);
                        } else {
                            PerkFeedbackMessageFactory.PerkFeedbackMessage feedback =
                                    PerkFeedbackMessageFactory.forFailure(perk, failure, data, player);
                            PacketDistributor.sendToPlayer(player, new PerkFeedbackPayload(
                                    feedback.title().getString(),
                                    feedback.message().getString()));
                        }
                        SyncHelper.syncPerks(player);
                    });
            registerEvent("net.puffish.skillsmod.api.Events$SkillLock", "registerSkillLockEvent",
                    (player, categoryId, skillId) -> {
                        if (isSyncing(player)) {
                            return;
                        }
                        SyncHelper.syncPerks(player);
                    });
            STATMod.LOGGER.info("Puffish Skills integration loaded");
        } catch (ReflectiveOperationException e) {
            loaded = false;
            STATMod.LOGGER.warn("Puffish Skills reflection failed: {}", e.getMessage());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void sync(ServerPlayer player, PlayerStatData data) {
        if (!loaded) {
            return;
        }

        UUID uuid = player.getUUID();
        SYNC_GUARD.add(uuid);
        try {
            PuffishSyncService.sync(data, new PuffishReflectionGateway(player));
        } finally {
            SYNC_GUARD.remove(uuid);
        }
    }

    public static void openScreen(ServerPlayer player) {
        if (!loaded) {
            return;
        }
        PuffishScreenService.open(new ReflectionScreenGateway(player));
    }

    private static boolean isSyncing(ServerPlayer player) {
        return player != null && SYNC_GUARD.contains(player.getUUID());
    }

    private static void registerEvent(String interfaceName, String registerMethodName, EventHandler handler)
            throws ReflectiveOperationException {
        Class<?> listenerInterface = Class.forName(interfaceName);
        Class<?> skillsApi = Class.forName(SKILLS_API_CLASS);
        Method registerMethod = skillsApi.getMethod(registerMethodName, listenerInterface);
        Object listener = Proxy.newProxyInstance(
                listenerInterface.getClassLoader(),
                new Class<?>[]{listenerInterface},
                (proxy, method, args) -> {
                    if (args != null && args.length == 3 && args[0] instanceof ServerPlayer player) {
                        handler.handle(player, String.valueOf(args[1]), String.valueOf(args[2]));
                    }
                    return null;
                });
        registerMethod.invoke(null, listener);
    }

    @FunctionalInterface
    private interface EventHandler {
        void handle(ServerPlayer player, String categoryId, String skillId);
    }

    private static final class ReflectionScreenGateway implements PuffishScreenGateway {
        private static final String SKILLS_MOD_CLASS = "net.puffish.skillsmod.SkillsMod";

        private final ServerPlayer player;

        private ReflectionScreenGateway(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public void refreshCategories() {
            invokeSkillsMod("updateAllCategories");
        }

        @Override
        public void openScreen() {
            try {
                Class<?> skillsApi = Class.forName(SKILLS_API_CLASS);
                Method openScreen = skillsApi.getMethod("openScreen", ServerPlayer.class);
                openScreen.invoke(null, player);
            } catch (ReflectiveOperationException e) {
                STATMod.LOGGER.warn("Puffish open screen failed for {}: {}", player.getName().getString(), e.getMessage());
            }
        }

        private void invokeSkillsMod(String methodName) {
            try {
                Class<?> skillsMod = Class.forName(SKILLS_MOD_CLASS);
                Method getInstance = skillsMod.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                Method method = skillsMod.getMethod(methodName, ServerPlayer.class);
                method.invoke(instance, player);
            } catch (ReflectiveOperationException e) {
                STATMod.LOGGER.warn("Puffish {} failed for {}: {}", methodName, player.getName().getString(), e.getMessage());
            }
        }
    }
}
