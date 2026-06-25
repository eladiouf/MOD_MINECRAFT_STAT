package tong.statmod.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;

import java.util.ArrayList;
import java.util.List;

public record PerkNodePresentation(
        PerkNodeVisualState state,
        List<Component> detailLines,
        Component statusLine) {

    public static PerkNodePresentation resolve(
            Perk perk,
            boolean unlocked,
            int statLevel,
            int familyPoints,
            int synergyLevel,
            boolean meetsExternalRequirements,
            List<String> externalRequirements) {
        if (perk == null) {
            return new PerkNodePresentation(
                    PerkNodeVisualState.LOCKED_PREREQ,
                    List.of(),
                    Component.literal("Blocked: requirements not met").withStyle(ChatFormatting.RED));
        }

        boolean meetsLevel = statLevel >= perk.tier.requiredStatLevel;
        boolean meetsPoints = familyPoints >= perk.tier.cost;
        boolean needsSynergy = perk.synergyStat != null;
        boolean meetsSynergy = !needsSynergy || synergyLevel >= PerkTier.SYNERGY.requiredStatLevel;
        boolean hasExternalRequirements = externalRequirements != null && !externalRequirements.isEmpty();
        boolean meetsExternal = !hasExternalRequirements || meetsExternalRequirements;

        List<Component> detailLines = new ArrayList<>();
        detailLines.add(requirementLine(
                "Stat requirement: " + statLevel + " / " + perk.tier.requiredStatLevel,
                meetsLevel));
        detailLines.add(requirementLine(
                "Perk points: " + familyPoints + " / " + perk.tier.cost,
                meetsPoints));
        if (needsSynergy) {
            detailLines.add(requirementLine(
                    perk.synergyStat.displayName + ": " + synergyLevel + " / " + PerkTier.SYNERGY.requiredStatLevel,
                    meetsSynergy));
        }
        if (hasExternalRequirements) {
            for (String requirement : externalRequirements) {
                detailLines.add(requirementLine("Requires: " + requirement, meetsExternalRequirements));
            }
        }

        if (unlocked) {
            return new PerkNodePresentation(
                    PerkNodeVisualState.UNLOCKED,
                    List.copyOf(detailLines),
                    Component.literal("Already unlocked").withStyle(ChatFormatting.GREEN));
        }

        int failureCount = 0;
        if (!meetsLevel) failureCount++;
        if (!meetsPoints) failureCount++;
        if (!meetsSynergy) failureCount++;
        if (!meetsExternal) failureCount++;

        if (failureCount == 0) {
            return new PerkNodePresentation(
                    PerkNodeVisualState.AVAILABLE,
                    List.copyOf(detailLines),
                    Component.literal("Click to unlock").withStyle(ChatFormatting.GREEN));
        }

        PerkNodeVisualState state = classifyState(meetsLevel, meetsPoints, meetsSynergy, meetsExternal, failureCount);
        return new PerkNodePresentation(state, List.copyOf(detailLines), Component.literal(statusText(meetsLevel, meetsPoints, meetsSynergy, meetsExternal)).withStyle(ChatFormatting.RED));
    }

    public List<Component> tooltipLines(Component name, Component description, Component tier, Component cost) {
        List<Component> lines = new ArrayList<>();
        lines.add(name);
        lines.add(description);
        lines.add(tier);
        lines.add(cost);
        lines.addAll(detailLines);
        lines.add(statusLine);
        return List.copyOf(lines);
    }

    private static PerkNodeVisualState classifyState(
            boolean meetsLevel,
            boolean meetsPoints,
            boolean meetsSynergy,
            boolean meetsExternal,
            int failureCount) {
        if (failureCount > 1) {
            return PerkNodeVisualState.LOCKED_MIXED;
        }
        if (!meetsExternal) {
            return PerkNodeVisualState.LOCKED_PREREQ;
        }
        if (!meetsPoints) {
            return PerkNodeVisualState.LOCKED_POINTS;
        }
        return PerkNodeVisualState.LOCKED_STAT;
    }

    private static String statusText(
            boolean meetsLevel,
            boolean meetsPoints,
            boolean meetsSynergy,
            boolean meetsExternal) {
        if (!meetsExternal) {
            return "Blocked: requirements not met";
        }
        if (!meetsLevel) {
            return "Blocked: stat level too low";
        }
        if (!meetsSynergy) {
            return "Blocked: synergy stat too low";
        }
        if (!meetsPoints) {
            return "Blocked: not enough perk points";
        }
        return "Blocked: requirements not met";
    }

    private static Component requirementLine(String text, boolean passed) {
        return Component.literal(text).withStyle(passed ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
