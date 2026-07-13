package tong.statmod.client.codex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.client.ClientMagicCache;
import tong.statmod.client.ClientStatCache;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import tong.statmod.magic.SchoolProgressTracker;
import tong.statmod.stats.StatType;

/**
 * Écran récap "Codex du Mage" — affiche d'un coup d'œil l'état magique du joueur.
 *
 * <p>Sections :
 * <ul>
 *   <li>Identité : race + branche de départ choisie</li>
 *   <li>Économie : magic points unifiés + niveaux ARCANE_POWER / ERUDITION</li>
 *   <li>Progression : mastery par école avec barre de progression vers le prochain palier</li>
 *   <li>Stats : nœuds débloqués / sorts appris</li>
 * </ul>
 *
 * <p>Ouvert via la commande {@code /magic codex} (S→C ouvre l'écran sur le client) ou
 * une keybind future. Lecture seule pour l'instant — actions futures (changer la branche
 * de départ, ouvrir le tree directement) ajoutables.
 */
@OnlyIn(Dist.CLIENT)
public final class MageCodexScreen extends Screen {

    private static final int VOLT = 0xFFD4FF00;
    private static final int VOLT_DIM = 0xFF7A9A00;
    private static final int OFF_WHITE = 0xFFFAFAFA;
    private static final int GRAY_300 = 0xFFD4D4D4;
    private static final int GRAY_500 = 0xFF808080;
    private static final int BACKGROUND = 0xCC0A0A0A;
    private static final int BORDER = 0xFF1A1A1A;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public MageCodexScreen() {
        super(Component.translatable("statmod.codex.title"));
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(360, this.width - 40);
        panelHeight = Math.min(280, this.height - 60);
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = (this.height - panelHeight) / 2;

        // Bouton "Change Start Branch" en bas-droite (Mission K)
        Button changeBranch = Button.builder(
                        Component.translatable("statmod.codex.change_branch"),
                        b -> this.minecraft.setScreen(new StartBranchChooserScreen(this)))
                .bounds(panelLeft + panelWidth - 130, panelTop + panelHeight - 26, 114, 18)
                .build();
        // Désactivé si pas de race choisie (pas de chooser possible)
        changeBranch.active = ClientMagicCache.getRaceOrdinal() >= 0;
        this.addRenderableWidget(changeBranch);
    }

    /**
     * 1.21 : dessiné dans renderBackground (après le flou du monde, avant les widgets)
     * — dans render() avant super.render(), la passe de fond de Screen flouterait le panneau.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Panel arrière-plan avec bordure
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, BACKGROUND);
        graphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, BORDER);
        // Bandeau Volt en haut pour le titre
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 20, BORDER);

        int x = panelLeft + 12;
        int y = panelTop + 6;

        // Titre
        graphics.drawString(this.font, Component.translatable("statmod.codex.title").getString(),
                x, y, VOLT, false);
        y += 22;

        // ---- Identité ----
        graphics.drawString(this.font, "§7" + Component.translatable("statmod.codex.identity").getString(),
                x, y, OFF_WHITE, false);
        y += 10;
        String raceName = getRaceName();
        String branchName = getStartBranchName();
        graphics.drawString(this.font, "§f" + raceName + "  §8·  §f" + branchName, x + 8, y, GRAY_300, false);
        y += 10;
        String raceSummary = getRaceSummary();
        if (!raceSummary.isBlank()) {
            graphics.drawWordWrap(this.font, Component.literal("§8" + raceSummary), x + 8, y, panelWidth - 32, GRAY_500);
            y += 24;
        } else {
            y += 14;
        }

        // ---- Économie ----
        graphics.drawString(this.font, "§7" + Component.translatable("statmod.codex.economy").getString(),
                x, y, OFF_WHITE, false);
        y += 10;
        int magicPoints = ClientMagicCache.getMagicPoints();
        graphics.drawString(this.font, "§e◆ §f" + magicPoints + " §7magic points",
                x + 8, y, GRAY_300, false);
        y += 10;
        int arcanePowerLv = ClientStatCache.getLevel(StatType.ARCANE_POWER.index);
        int eruditionLv = ClientStatCache.getLevel(StatType.ERUDITION.index);
        graphics.drawString(this.font,
                "§bArcane Power §7Lv§r " + arcanePowerLv + "  §bErudition §7Lv§r " + eruditionLv,
                x + 8, y, GRAY_300, false);
        y += 14;

        // ---- Progression mastery ----
        graphics.drawString(this.font, "§7" + Component.translatable("statmod.codex.mastery").getString(),
                x, y, OFF_WHITE, false);
        y += 10;
        for (MagicBranch branch : MagicBranch.values()) {
            if (branch == MagicBranch.COMMON) continue;
            int progressionMastery = ClientMagicCache.getMasteryProgress(branch);
            int practiceMastery = ClientMagicCache.getPracticeMasteryProgress(branch);
            long totalMastery = (long) progressionMastery + practiceMastery;
            int nextMilestone = nextMilestone(progressionMastery);
            String suffix = nextMilestone > 0
                    ? " §8→ next palier " + nextMilestone
                    : " §8(max paliers)";
            graphics.drawString(this.font,
                    "§f" + capitalize(branch.id) + " §7Lv§r " + (totalMastery / 100) + "§7." + (totalMastery % 100) + suffix,
                    x + 8, y, GRAY_300, false);
            y += 9;
        }
        y += 4;

        // ---- Compteurs ----
        graphics.drawString(this.font, "§7" + Component.translatable("statmod.codex.unlocked").getString(),
                x, y, OFF_WHITE, false);
        y += 10;
        graphics.drawString(this.font, "§a✓§r " + ClientMagicCache.getMagicNodeCount() + " nodes  §7·§r "
                        + ClientMagicCache.getLearnedSpellsCount() + " spells learned",
                x + 8, y, GRAY_300, false);
        y += 18;

        // ---- Footer hint ----
        graphics.drawString(this.font,
                "§8" + Component.translatable("statmod.codex.hint_close").getString(),
                panelLeft + 12, panelTop + panelHeight - 14, GRAY_500, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String getRaceName() {
        int ord = ClientMagicCache.getRaceOrdinal();
        if (ord < 0 || ord >= MagicRace.values().length) return "§7—";
        return MagicRace.values()[ord].displayName();
    }

    private static String getRaceSummary() {
        int ord = ClientMagicCache.getRaceOrdinal();
        if (ord < 0 || ord >= MagicRace.values().length) return "";
        return MagicRace.values()[ord].summary();
    }

    private static String getStartBranchName() {
        int ord = ClientMagicCache.getStartBranchOrdinal();
        if (ord < 0 || ord >= MagicBranch.values().length) return "§7—";
        return capitalize(MagicBranch.values()[ord].id);
    }

    /**
     * Retourne le prochain seuil de mastery non encore franchi, ou 0 si tous les paliers sont
     * atteints. Lit la table {@link SchoolProgressTracker#MILESTONE_THRESHOLDS}.
     */
    private static int nextMilestone(int mastery) {
        for (int t : SchoolProgressTracker.MILESTONE_THRESHOLDS) {
            if (mastery < t) return t;
        }
        return 0;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
