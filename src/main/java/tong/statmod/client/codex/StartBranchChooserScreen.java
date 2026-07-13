package tong.statmod.client.codex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.client.ClientMagicCache;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import tong.statmod.network.ChangeStartBranchPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Sous-écran depuis {@link MageCodexScreen} qui permet au joueur de choisir/changer sa
 * branche de départ parmi les écoles compatibles avec sa race
 * ({@link MagicRace#canChooseStartBranch(MagicBranch)}).
 *
 * <p>Click sur un bouton envoie {@link ChangeStartBranchPayload} au serveur qui valide
 * une seconde fois (race compatibility). La sync magic re-render le Codex au retour.
 */
@OnlyIn(Dist.CLIENT)
public final class StartBranchChooserScreen extends Screen {

    private static final int VOLT = 0xFFD4FF00;
    private static final int OFF_WHITE = 0xFFFAFAFA;
    private static final int GRAY_300 = 0xFFD4D4D4;
    private static final int GRAY_500 = 0xFF808080;
    private static final int BACKGROUND = 0xCC0A0A0A;
    private static final int BORDER = 0xFF1A1A1A;

    private final Screen parent;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public StartBranchChooserScreen(Screen parent) {
        super(Component.translatable("statmod.codex.choose_branch"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(300, this.width - 60);
        panelHeight = Math.min(240, this.height - 80);
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = (this.height - panelHeight) / 2;

        MagicRace race = currentRace();
        List<MagicBranch> compatible = compatibleBranches(race);

        // Boutons par branche compatible (verticaux)
        int btnTop = panelTop + 74;
        int btnHeight = 18;
        int btnSpacing = 4;
        int btnWidth = panelWidth - 32;
        int currentStart = ClientMagicCache.getStartBranchOrdinal();

        for (int i = 0; i < compatible.size(); i++) {
            MagicBranch branch = compatible.get(i);
            boolean isCurrent = currentStart == branch.ordinal();
            boolean isDefault = race != null && branch == race.defaultStartBranch();
            String label = branchLabel(branch, isCurrent, isDefault);
            Button btn = Button.builder(Component.literal(label), b -> sendChangeRequest(branch))
                    .bounds(panelLeft + 16, btnTop + i * (btnHeight + btnSpacing), btnWidth, btnHeight)
                    .build();
            btn.active = !isCurrent;
            this.addRenderableWidget(btn);
        }

        // Bouton retour
        Button back = Button.builder(Component.literal("§7← Back"),
                        b -> this.minecraft.setScreen(parent))
                .bounds(panelLeft + 16, panelTop + panelHeight - 26, panelWidth - 32, 18)
                .build();
        this.addRenderableWidget(back);
    }

    private void sendChangeRequest(MagicBranch branch) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new ChangeStartBranchPayload(branch.ordinal()));
        // Retour au codex parent (qui se re-render avec la nouvelle valeur après sync).
        this.minecraft.setScreen(parent);
    }

    /**
     * 1.21 : dessiné dans renderBackground (après le flou du monde, avant les widgets)
     * — dans render() avant super.render(), la passe de fond de Screen flouterait le panneau.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, BACKGROUND);
        graphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, BORDER);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 24, BORDER);

        graphics.drawString(this.font,
                Component.translatable("statmod.codex.choose_branch").getString(),
                panelLeft + 12, panelTop + 8, VOLT, false);

        MagicRace race = currentRace();
        String raceLine = race != null
                ? "§7Race: §f" + race.displayName() + "  §8(affinities: " + affinitiesString(race) + "§8)"
                : "§7" + Component.translatable("statmod.codex.no_race").getString();
        graphics.drawString(this.font, raceLine, panelLeft + 12, panelTop + 28, GRAY_300, false);
        if (race != null) {
            graphics.drawWordWrap(this.font, Component.literal("§8" + race.summary()), panelLeft + 12, panelTop + 42,
                    panelWidth - 24, GRAY_500);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static MagicRace currentRace() {
        int ord = ClientMagicCache.getRaceOrdinal();
        if (ord < 0 || ord >= MagicRace.values().length) return null;
        return MagicRace.values()[ord];
    }

    private static List<MagicBranch> compatibleBranches(MagicRace race) {
        List<MagicBranch> result = new ArrayList<>();
        if (race == null) return result;
        for (MagicBranch b : MagicBranch.values()) {
            if (race.canChooseStartBranch(b)) {
                result.add(b);
            }
        }
        return result;
    }

    private static String affinitiesString(MagicRace race) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (MagicBranch b : race.orderedAffinities()) {
            if (!first) sb.append("§8, ");
            sb.append("§f").append(capitalize(b.id));
            first = false;
        }
        return sb.toString();
    }

    private static String branchLabel(MagicBranch branch, boolean current, boolean isDefault) {
        StringBuilder label = new StringBuilder(capitalize(branch.id));
        if (current) {
            label.append(" §a(current)");
        } else if (isDefault) {
            label.append(" §8(preferred)");
        }
        return label.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
