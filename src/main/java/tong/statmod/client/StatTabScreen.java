package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.puffish.PuffishSkillsCompat;
import tong.statmod.integration.PlayerDataBridge;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.tensura.TensuraToMagicRaceMapper;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatFamily;
import tong.statmod.storage.PlayerStatData;
import tong.statmod.stats.StatCombatScaling;
import tong.statmod.stats.CraftingSupportEffectHandler;

import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class StatTabScreen extends Screen {
    private int scrollOffset;
    private StatType selectedStat = StatType.BRUTE_FORCE;

    public StatTabScreen() {
        super(Component.translatable("statmod.tab.stats"));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(partialTick);
        // Fond sombre premium (violet foncé / gris ardoise)
        graphics.fill(0, 0, width, height, 0xDD0D0D16);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int soulLevel = ClientStatCache.getSoulLevel();
        int maxStat = soulLevel > 0 ? Math.min(soulLevel, 100) : 100;

        String race = "Unknown";
        try {
            String r = PlayerDataBridge.getRaceId(player);
            race = TensuraToMagicRaceMapper.displayRaceName(r);
        } catch (Exception ignored) {}

        int cx = width / 2;

        // ── EN-TÊTE PREMIUM ──────────────────────────────────────────────────
        String title = "✦ PANNEAU DE STATISTIQUES ✦";
        graphics.drawString(font, title, cx - font.width(title) / 2, 12, 0xFFFFAA00);

        String subtitle = "Race: §f" + race + "§r  |  Soul Lv: §e" + soulLevel + "§r  |  Cap Max: §c" + maxStat;
        graphics.drawString(font, subtitle, cx - font.width(subtitle) / 2, 25, 0xFF888888);

        // ── DIVISEURS ET PANNEAUX ───────────────────────────────────────────
        int leftPanelW = (int) (width * 0.52f);
        int rightPanelW = (int) (width * 0.42f);
        int gap = (int) (width * 0.03f);
        
        int startX = (width - (leftPanelW + rightPanelW + gap)) / 2;
        int leftX = startX;
        int rightX = startX + leftPanelW + gap;
        int topY = 40;
        int bottomY = height - 35;
        int panelH = bottomY - topY;

        // Tracé des cadres des panneaux
        graphics.fill(leftX - 4, topY - 4, leftX + leftPanelW + 4, bottomY + 4, 0x55222233);
        graphics.fill(rightX - 4, topY - 4, rightX + rightPanelW + 4, bottomY + 4, 0x55222233);
        
        // Bordures fines dorées pour le panneau de détails
        graphics.fill(rightX - 4, topY - 4, rightX + rightPanelW + 4, topY - 3, 0xAAFFAA00);
        graphics.fill(rightX - 4, bottomY + 3, rightX + rightPanelW + 4, bottomY + 4, 0xAAFFAA00);

        // ── COLONNE DE GAUCHE : LISTE DES STATS AVEC PROGRESSION D'XP ─────────
        int totalStats = StatType.values().length;
        int visibleCount = (panelH - 10) / 22; // statistiques visibles à la fois (environ 8 à 10)
        int maxScroll = Math.max(0, totalStats - visibleCount);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);

        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + visibleCount, totalStats);

        int itemY = topY + 5;
        for (int i = startIdx; i < endIdx; i++) {
            StatType stat = StatType.values()[i];
            int level = RaceEffectApplier.getEffectiveLevel(player, stat.index);
            int baseLvl = player.getData(tong.statmod.storage.ModAttachments.STATS).getLevel(stat.index);
            int xp = ClientStatCache.getXp(stat.index);
            int needed = PlayerStatData.requiredXp(baseLvl);
            
            boolean isSelected = (stat == selectedStat);
            boolean isHovered = mouseX >= leftX && mouseX < leftX + leftPanelW && mouseY >= itemY && mouseY < itemY + 20;

            // Fond lors de la sélection ou survol
            int bgCol = isSelected ? 0x44FFAA00 : (isHovered ? 0x22FFFFFF : 0x11FFFFFF);
            graphics.fill(leftX, itemY, leftX + leftPanelW, itemY + 20, bgCol);
            if (isSelected) {
                // Petite barre jaune de sélection sur le bord gauche
                graphics.fill(leftX, itemY, leftX + 3, itemY + 20, 0xFFFFAA00);
            }

            // Couleur du texte en fonction de la famille
            int statColor = getFamilyColor(stat.family);
            String statNameText = (baseLvl < level) ? stat.displayName + " §a(+" + (level - baseLvl) + ")" : stat.displayName;
            graphics.drawString(font, statNameText, leftX + 8, itemY + 3, statColor);

            // Niveau à droite
            String lvlText = "Lv." + level;
            graphics.drawString(font, lvlText, leftX + leftPanelW - font.width(lvlText) - 8, itemY + 3, 0xFFFFFFFF);

            // Barre de progression d'XP en dessous
            int barWidth = leftPanelW - 16;
            int barX = leftX + 8;
            int barY = itemY + 14;
            int barH = 3;
            graphics.fill(barX, barY, barX + barWidth, barY + barH, 0xFF333333); // Fond de la barre
            
            float progress = needed > 0 ? (float) xp / needed : 0.0f;
            int filledWidth = (int) (barWidth * Math.clamp(progress, 0.0f, 1.0f));
            graphics.fill(barX, barY, barX + filledWidth, barY + barH, statColor); // Remplissage coloré

            itemY += 22;
        }

        // ── PANNEAU DE DROITE : DÉTAILS DE LA STAT SÉLECTIONNÉE ─────────────
        if (selectedStat != null) {
            int detailY = topY + 8;
            
            // Titre de la Stat
            String statTitle = selectedStat.displayName.toUpperCase(java.util.Locale.ROOT);
            graphics.drawString(font, "§6§l" + statTitle, rightX + 8, detailY, 0xFFFFAA00);
            detailY += 15;

            // Famille
            String familyName = "Famille: " + getFamilyDisplayName(selectedStat.family);
            graphics.drawString(font, "§8" + familyName, rightX + 8, detailY, 0xFF888888);
            detailY += 15;

            // Séparateur
            graphics.fill(rightX + 8, detailY, rightX + rightPanelW - 8, detailY + 1, 0x33FFFFFF);
            detailY += 8;

            // Description générale de la stat
            List<FormattedCharSequence> descLines = font.split(Component.literal(selectedStat.description), rightPanelW - 16);
            for (var line : descLines) {
                graphics.drawString(font, line, rightX + 8, detailY, 0xFFDDDDDD);
                detailY += 10;
            }
            detailY += 5;

            // Niveaux (Base / Actuel)
            int baseLvl = player.getData(tong.statmod.storage.ModAttachments.STATS).getLevel(selectedStat.index);
            int currentLvl = RaceEffectApplier.getEffectiveLevel(player, selectedStat.index);
            graphics.drawString(font, "Niveau de Base: §e" + baseLvl, rightX + 8, detailY, 0xFFFFFFFF);
            detailY += 10;
            graphics.drawString(font, "Niveau Effectif: §a" + currentLvl + (currentLvl > baseLvl ? " §7(Bonus de Race)" : ""), rightX + 8, detailY, 0xFFFFFFFF);
            detailY += 15;

            // Barre de séparation
            graphics.fill(rightX + 8, detailY, rightX + rightPanelW - 8, detailY + 1, 0x33FFFFFF);
            detailY += 8;

            // Effets Passifs en fonction de la stat sélectionnée
            graphics.drawString(font, "§e§nBonus Passif Actuel :", rightX + 8, detailY, 0xFFFFAA00);
            detailY += 12;
            
            List<String> passiveEffects = getPassiveEffectsDescription(selectedStat, currentLvl);
            for (String effect : passiveEffects) {
                List<FormattedCharSequence> effectWrapped = font.split(Component.literal(effect), rightPanelW - 16);
                for (var line : effectWrapped) {
                    graphics.drawString(font, line, rightX + 8, detailY, 0xFFCCCCCC);
                    detailY += 10;
                }
            }
            detailY += 10;

            // Barre de séparation
            graphics.fill(rightX + 8, detailY, rightX + rightPanelW - 8, detailY + 1, 0x33FFFFFF);
            detailY += 8;

            // Milestones et Perks
            graphics.drawString(font, "§e§nMilestones Débloqués :", rightX + 8, detailY, 0xFFFFAA00);
            detailY += 12;

            String m10 = (currentLvl >= 10 ? "§a✔" : "§c❌") + " §7Niveau 10: §fApprenti";
            graphics.drawString(font, m10, rightX + 8, detailY, 0xFFFFFFFF);
            detailY += 10;

            String m25 = (currentLvl >= 25 ? "§a✔" : "§c❌") + " §7Niveau 25: §fAdepte (Buff)";
            graphics.drawString(font, m25, rightX + 8, detailY, 0xFFFFFFFF);
            detailY += 10;

            String m50 = (currentLvl >= 50 ? "§a✔" : "§c❌") + " §7Niveau 50: §fExpert (Buff puissant)";
            graphics.drawString(font, m50, rightX + 8, detailY, 0xFFFFFFFF);
            detailY += 10;

            String m75 = (currentLvl >= 75 ? "§a✔" : "§c❌") + " §7Niveau 75: §fMaître (Server broadcast)";
            graphics.drawString(font, m75, rightX + 8, detailY, 0xFFFFFFFF);
            detailY += 10;

            String m100 = (currentLvl >= 100 ? "§a✔" : "§c❌") + " §7Niveau 100: §6Légende";
            graphics.drawString(font, m100, rightX + 8, detailY, 0xFFFFFFFF);
        }

        // Navigation aide
        String nav = "ESC fermer  |  P ou Bouton haut pour l'Arbre de Perks";
        graphics.drawString(font, Component.literal(nav), cx - font.width(nav) / 2, height - 20, 0xFF888888);

        // Bouton rapide Perk Tree
        String btnLabel = "§e✦ Perk Tree ✦§r";
        int btnW = font.width(btnLabel) + 12;
        int btnH = 14;
        int btnX = rightX + rightPanelW - btnW;
        int btnY = 12;
        boolean hover = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, hover ? 0xFF445588 : 0xFF223366);
        graphics.drawString(font, Component.literal(btnLabel), btnX + 6, btnY + 3, 0xFFFFAA00);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int cx = width / 2;
            int leftPanelW = (int) (width * 0.52f);
            int rightPanelW = (int) (width * 0.42f);
            int gap = (int) (width * 0.03f);
            int startX = (width - (leftPanelW + rightPanelW + gap)) / 2;
            int leftX = startX;
            int rightX = startX + leftPanelW + gap;

            // Click sur le bouton Perk Tree
            String btnLabel = "§e✦ Perk Tree ✦§r";
            int btnW = font.width(btnLabel) + 12;
            int btnH = 14;
            int btnX = rightX + rightPanelW - btnW;
            int btnY = 12;
            if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                PerkUiRouter.openFromClient(PuffishSkillsCompat.isLoaded());
                return true;
            }

            // Click sur la liste des stats de gauche
            int topY = 40;
            int bottomY = height - 35;
            int panelH = bottomY - topY;
            int visibleCount = (panelH - 10) / 22;

            int itemY = topY + 5;
            int totalStats = StatType.values().length;
            int endIdx = Math.min(scrollOffset + visibleCount, totalStats);

            for (int i = scrollOffset; i < endIdx; i++) {
                if (mouseX >= leftX && mouseX < leftX + leftPanelW && mouseY >= itemY && mouseY < itemY + 20) {
                    selectedStat = StatType.values()[i];
                    return true;
                }
                itemY += 22;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int leftPanelW = (int) (width * 0.52f);
        int leftX = (width - (leftPanelW + (int) (width * 0.42f) + (int) (width * 0.03f))) / 2;

        // Ne scroller la liste de gauche que si la souris est au-dessus
        if (mouseX >= leftX && mouseX < leftX + leftPanelW) {
            int topY = 40;
            int bottomY = height - 35;
            int panelH = bottomY - topY;
            int visibleCount = (panelH - 10) / 22;
            int totalStats = StatType.values().length;
            int maxScroll = Math.max(0, totalStats - visibleCount);
            
            scrollOffset = Math.clamp(scrollOffset - (int) scrollY, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        // ESC (256) ou touche K (75) / autre pour fermer
        if (key == 256 || key == 75) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    // ── HELPERS POUR COULEURS ET DESCRIPTIONS DÉTAILLÉES ──────────────────────

    private int getFamilyColor(StatFamily family) {
        return switch (family) {
            case FRONTLINE_PHYSICAL_COMBAT -> 0xFFFF5555; // Rouge vif
            case RANGED_HUNT_CONTROL -> 0xFF55FF55;       // Vert
            case MAGICAL_CORE -> 0xFFAA55FF;              // Violet
            case ELEMENTAL_SPECIALIZATION -> 0xFF55FFFF;  // Cyan
            case CRAFTING_SUPPORT -> 0xFFFFaa00;          // Doré/Orange
            case MENTAL_PRESSURE_RESILIENCE -> 0xFFFFFFFF; // Blanc
        };
    }

    private String getFamilyDisplayName(StatFamily family) {
        return switch (family) {
            case FRONTLINE_PHYSICAL_COMBAT -> "Combat Physique de Mêlée";
            case RANGED_HUNT_CONTROL -> "Combat à Distance & Traque";
            case MAGICAL_CORE -> "Noyau Magique & Arcanes";
            case ELEMENTAL_SPECIALIZATION -> "Spécialisation Élémentaire";
            case CRAFTING_SUPPORT -> "Artisanat et Alchimie";
            case MENTAL_PRESSURE_RESILIENCE -> "Résilience Mentale";
        };
    }

    private List<String> getPassiveEffectsDescription(StatType stat, int level) {
        List<String> list = new ArrayList<>();
        switch (stat) {
            case BRUTE_FORCE -> {
                float mult = StatCombatScaling.weaponDamageMultiplier(stat, level, 0, 0, 0, 0, 1.5f, 3.0f);
                list.add(String.format("Multiplicateur dégâts haches/masses: x%.2f", mult));
            }
            case BLADE_TECHNIQUE -> {
                float mult = StatCombatScaling.weaponDamageMultiplier(stat, 0, level, 0, 0, 0, 1.5f, 3.0f);
                list.add(String.format("Multiplicateur dégâts épées/lames: x%.2f", mult));
            }
            case RAPIDITE -> {
                float mult = StatCombatScaling.weaponDamageMultiplier(stat, 0, 0, 0, level, 0, 1.5f, 3.0f);
                list.add(String.format("Multiplicateur dégâts dagues/rapides: x%.2f", mult));
                list.add(String.format("Chance de double-frappe: %.1f%%", level * 0.2f));
            }
            case AGILITY -> {
                list.add(String.format("Vitesse de déplacement: +%.1f%%", level * 0.1f));
                list.add(String.format("Distance de chute réduite: -%.1f%%", level * 0.5f));
                list.add(String.format("Dégâts bonus en mouvement: +%.1f%%", level * 0.2f));
            }
            case PHYSICAL_RESISTANCE -> {
                list.add(String.format("Réduction dégâts physiques subis: -%.1f%%", Math.min(50.0f, level * 0.5f)));
            }
            case PHYSICAL_ENDURANCE -> {
                list.add(String.format("Réduction dégâts subis (cap 30%%): -%.1f%%", Math.min(30.0f, level * 0.2f)));
                list.add(String.format("Dégâts de chute subis: -%.1f%%", level * 0.3f));
            }
            case PRECISION -> {
                float mult = StatCombatScaling.weaponDamageMultiplier(stat, 0, 0, level, 0, 0, 1.5f, 3.0f);
                list.add(String.format("Multiplicateur dégâts projectiles: x%.2f", mult));
                list.add("Niveau 50+: +15% de dégâts critiques à vie max.");
            }
            case ARCANE_POWER -> {
                float mult = StatCombatScaling.weaponDamageMultiplier(stat, 0, 0, 0, 0, level, 1.5f, 3.0f);
                list.add(String.format("Multiplicateur dégâts sorts/magie: x%.2f", mult));
            }
            case WATER_AFFINITY -> {
                list.add("Régénère la vie sous l'eau ou la pluie (dès le niv. 5).");
                list.add("Permet de respirer indéfiniment sous l'eau (niv. 10+).");
            }
            case EARTH_AFFINITY -> {
                list.add(String.format("Résistance passive au recul (knockback): +%.1f%%", Math.min(50.0f, level * 0.5f)));
            }
            case FIRE_AFFINITY -> {
                list.add("Résistance permanente aux brûlures (dès le niv. 30).");
                list.add("Diminue le temps de combustion quand enflammé (niv. 15+).");
            }
            case AIR_AFFINITY -> {
                list.add("Donne un effet permanent de Jump Boost I/II (dès le niv. 5).");
                list.add("Donne un effet de Slow Falling lors des grandes chutes (niv. 20+).");
            }
            case MAGIC_RESISTANCE -> {
                list.add(String.format("Réduction dégâts magiques subis: -%.1f%%", Math.min(50.0f, level * 0.5f)));
            }
            case CASTING_SPEED -> {
                list.add("Donne Haste I/II permanent (niv. 10+) pour miner et attaquer.");
            }
            case MANA_POOL -> {
                list.add("Génère passivement de l'absorption toutes les secondes.");
                list.add(String.format("Cœur d'absorption max: %.1f cœurs", Math.min(10.0f, level * 0.2f)));
            }
            case ERUDITION -> {
                list.add(String.format("Bonus d'expérience vanilla gagnée: +%.2f%%", level * 0.15f));
            }
            case TRACKING -> {
                list.add(String.format("Durée de marquage (Glow) sur coup: %d ticks", level * 4));
            }
            case KEEN_SENSES -> {
                list.add(String.format("Chances passives d'esquive de dégâts: %.1f%%", level * 0.2f));
            }
            case FORGING -> {
                list.add(String.format("Réduction perte de durabilité d'outils: -%.1f%%", Math.min(90.0f, level * 1.0f)));
            }
            case COOKING -> {
                float saturation = CraftingSupportEffectHandler.foodSaturationBonus(level, false);
                list.add(String.format("Saturation bonus lors de repas: +%.2f", saturation));
            }
            case ALCHEMY -> {
                list.add(String.format("Augmentation durée des potions: +%.1f%%", level * 1.0f));
            }
            case INTIMIDATION -> {
                list.add(String.format("Dégâts accrus contre les cibles marquées: +%.1f%%", level * 0.5f));
            }
            case WILLPOWER -> {
                list.add(String.format("Réduction durée des effets négatifs (cap 30%%): -%.1f%%", Math.min(30.0f, level * 0.3f)));
            }
        }
        if (list.isEmpty()) {
            list.add("Aucun effet passif direct (sert de palier pour l'arbre de compétences).");
        }
        return list;
    }
}
