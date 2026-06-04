package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tong.statmod.Config;
import tong.statmod.ConfigPresets;
import java.util.*;

public class ConfigScreen extends Screen {
    private static final int BG_COLOR = 0xE0000000;
    private static final int LINE_HEIGHT = 24;
    private int scrollOffset = 0;
    private final List<ConfigRow> rows = new ArrayList<>();
    private boolean dirty = false;

    public ConfigScreen() {
        super(Component.literal("STAT Mod Configuration"));
        buildRows();
    }

    private void buildRows() {
        rows.clear();
        rows.add(row("Debug", "enableDebug", Config.enableDebug, "Enable debug logging"));
        rows.add(intRow("XP Multiplier", "xpPerLevelMultiplier", Config.xpPerLevelMultiplier, "XP per level = (level+1)*value"));
        rows.add(intRow("XP Tier Common Min", "xpTierCommonMin", Config.xpTierCommonMin, "Min XP for common actions"));
        rows.add(intRow("XP Tier Common Max", "xpTierCommonMax", Config.xpTierCommonMax, "Max XP for common actions"));
        rows.add(intRow("XP Tier Int. Min", "xpTierIntermediateMin", Config.xpTierIntermediateMin, "Min XP for intermediate actions"));
        rows.add(intRow("XP Tier Int. Max", "xpTierIntermediateMax", Config.xpTierIntermediateMax, "Max XP for intermediate actions"));
        rows.add(intRow("XP Tier Rare Min", "xpTierRareMin", Config.xpTierRareMin, "Min XP for rare actions"));
        rows.add(intRow("XP Tier Rare Max", "xpTierRareMax", Config.xpTierRareMax, "Max XP for rare actions"));
        rows.add(intRow("Weapon XP Min", "weaponXpMin", Config.weaponXpMin, "Min weapon XP per hit"));
        rows.add(intRow("Weapon XP Max", "weaponXpMax", Config.weaponXpMax, "Max weapon XP per hit"));
        rows.add(intRow("Weapon Max Level", "weaponMasteryMaxLevel", Config.weaponMasteryMaxLevel, "Max weapon mastery level"));
        rows.add(intRow("Perk Tier 1 Level", "perkTier1Level", Config.perkTier1Level, "Level for tier 1 perks"));
        rows.add(intRow("Perk Tier 2 Level", "perkTier2Level", Config.perkTier2Level, "Level for tier 2 perks"));
        rows.add(intRow("Perk Tier 3 Level", "perkTier3Level", Config.perkTier3Level, "Level for tier 3 perks"));
        rows.add(intRow("Skill Tier 1 Level", "skillTier1Level", Config.skillTier1Level, "Level for tier 1 skills"));
        rows.add(intRow("Skill Tier 2 Level", "skillTier2Level", Config.skillTier2Level, "Level for tier 2 skills"));
        rows.add(intRow("Skill Tier 3 Level", "skillTier3Level", Config.skillTier3Level, "Level for tier 3 skills"));
        rows.add(intRow("Skill Active Level", "skillActiveLevel", Config.skillActiveLevel, "Level for active skills"));
        rows.add(dRow("Cooldown Multiplier", "cooldownMultiplier", Config.cooldownMultiplier, "Global skill cooldown multiplier"));
        rows.add(dRow("Fatigue Day Rate", "fatigueDayRate", Config.fatigueDayRate, "Fatigue per tick during day"));
        rows.add(dRow("Fatigue Night Rate", "fatigueNightRate", Config.fatigueNightRate, "Fatigue per tick during night"));
        rows.add(dRow("Fatigue Underground", "fatigueUndergroundRate", Config.fatigueUndergroundRate, "Fatigue per tick underground"));
        rows.add(intRow("Fatigue Max Capacity", "fatigueMaxCapacity", Config.fatigueMaxCapacity, "Max fatigue capacity"));
        rows.add(dRow("Thirst Base Decay", "thirstBaseDecay", Config.thirstBaseDecay, "Base thirst decay per tick"));
        rows.add(dRow("Thirst Sprint Cost", "thirstSprintCost", Config.thirstSprintCost, "Thirst cost per sprint tick"));
    }

    private ConfigRow row(String label, String key, boolean value, String desc) {
        return new ConfigRow(label, key, String.valueOf(value), desc);
    }
    private ConfigRow intRow(String label, String key, int value, String desc) {
        return new ConfigRow(label, key, String.valueOf(value), desc);
    }
    private ConfigRow dRow(String label, String key, double value, String desc) {
        return new ConfigRow(label, key, String.format("%.4f", value), desc);
    }

    @Override
    protected void init() {
        super.init();
        int y = 50 - scrollOffset;
        for (ConfigRow row : rows) {
            int inputX = this.width / 2 + 20;
            EditBox box = new EditBox(this.font, inputX, y, 80, 18, Component.literal(row.key));
            box.setValue(row.value);
            box.setResponder(v -> { dirty = true; row.value = v; });
            addRenderableWidget(box);
            addRenderableWidget(Button.builder(Component.literal("+"), b -> {})
                .pos(inputX + 85, y).size(18, 18).build());
            addRenderableWidget(Button.builder(Component.literal("-"), b -> {})
                .pos(inputX + 105, y).size(18, 18).build());
            y += LINE_HEIGHT;
        }
        addRenderableWidget(Button.builder(Component.literal("Save & Apply"), b -> saveConfig())
            .pos(this.width / 2 - 100, this.height - 40).size(95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Defaults"), b -> resetDefaults())
            .pos(this.width / 2 + 5, this.height - 40).size(95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
            .pos(this.width / 2 - 50, this.height - 18).size(100, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Easy"), b -> applyPreset(ConfigPresets.Preset.EASY))
            .pos(this.width / 2 - 100, 30).size(60, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Normal"), b -> applyPreset(ConfigPresets.Preset.NORMAL))
            .pos(this.width / 2 - 30, 30).size(60, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Hard"), b -> applyPreset(ConfigPresets.Preset.HARD))
            .pos(this.width / 2 + 40, 30).size(60, 16).build());
    }

    private void saveConfig() { dirty = false; }

    private void resetDefaults() { dirty = true; buildRows(); rebuildWidgets(); }

    private void applyPreset(ConfigPresets.Preset preset) {
        ConfigPresets.applyPreset(Minecraft.getInstance().player, preset);
        buildRows();
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BG_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);
        int y = 50 - scrollOffset;
        for (ConfigRow row : rows) {
            graphics.drawString(this.font, row.label + ": " + row.value, 10, y + 5, 0xFFFFFFFF);
            y += LINE_HEIGHT;
        }
        graphics.drawCenteredString(this.font, "STAT Mod Configuration", this.width / 2, 10, 0xFFFFAA00);
    }

    @Override
    public boolean isPauseScreen() { return true; }

    private static class ConfigRow {
        final String label, key, desc;
        String value;
        ConfigRow(String l, String k, String v, String d) { label = l; key = k; value = v; desc = d; }
    }
}
