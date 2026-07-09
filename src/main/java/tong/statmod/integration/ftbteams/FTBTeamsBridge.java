package tong.statmod.integration.ftbteams;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Intégration optionnelle FTB Teams (2026-07-09) — la notion d'« équipe » du donjon.
 *
 * <p>Le gameplay co-op du Trial Dungeon (conquête partagée, points d'assist) privilégie
 * l'<b>équipe FTB</b> du joueur qui accomplit l'objectif : ses coéquipiers présents sur l'étage
 * profitent de la conquête et des assists, pas les joueurs rivaux qui traînent au même endroit.
 *
 * <p>Sans FTB Teams (ou en cas d'erreur du mod), on retombe sur le comportement « tout le monde
 * co-op » : deux joueurs sur un étage sont considérés co-équipiers. FTB Teams donne à chaque
 * joueur une équipe personnelle par défaut → deux joueurs sans party ne sont PAS co-équipiers,
 * ce qui est le comportement voulu (« fais /ftbteams party create pour jouer ensemble »).
 *
 * <p>Pattern maison : classe garde (jamais de classe FTB chargée si le mod est absent) +
 * classe interne {@code Hook} qui référence l'API réelle.
 */
public final class FTBTeamsBridge {

    private static Boolean loaded;

    private FTBTeamsBridge() {}

    /** {@code true} si FTB Teams est présent (résolu une fois). */
    public static boolean loaded() {
        if (loaded == null) loaded = ModList.get().isLoaded("ftbteams");
        return loaded;
    }

    /**
     * {@code true} si {@code a} et {@code b} sont dans la même équipe FTB.
     * Sans FTB Teams : toujours {@code true} (tout le monde co-op, comportement historique).
     */
    public static boolean sameTeam(ServerPlayer a, ServerPlayer b) {
        if (a == b) return true;
        if (!loaded()) return true;
        try {
            return Hook.sameTeam(a, b);
        } catch (Throwable t) {
            return true; // une erreur FTB ne doit jamais casser la conquête
        }
    }

    /** Nom de l'équipe du joueur (pour les messages de victoire), ou {@code null}. */
    public static Component teamName(ServerPlayer player) {
        if (!loaded()) return null;
        try {
            return Hook.teamName(player);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Seule classe à toucher l'API FTB Teams — chargée uniquement si le mod est présent. */
    private static final class Hook {
        static boolean sameTeam(ServerPlayer a, ServerPlayer b) {
            var api = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api();
            if (!api.isManagerLoaded()) return true;
            return api.getManager().arePlayersInSameTeam(a.getUUID(), b.getUUID());
        }

        static Component teamName(ServerPlayer player) {
            var api = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api();
            if (!api.isManagerLoaded()) return null;
            return api.getManager().getTeamForPlayer(player)
                    .map(dev.ftb.mods.ftbteams.api.Team::getName)
                    .orElse(null);
        }
    }
}
