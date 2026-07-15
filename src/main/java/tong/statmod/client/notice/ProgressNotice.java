package tong.statmod.client.notice;

import tong.statmod.network.StatProgressNoticeMessage;
import tong.statmod.stats.StatType;

public record ProgressNotice(
        StatType stat,
        int awardedXp,
        int newLevel,
        int levelsGained,
        long updatedAt,
        int remainingTicks) {
    ProgressNotice merge(StatProgressNoticeMessage message, long tick) {
        long combinedXp = (long) awardedXp + message.awardedXp();
        return new ProgressNotice(
                stat,
                (int) Math.min(combinedXp, StatProgressNoticeMessage.MAX_AWARDED_XP),
                Math.max(newLevel, message.newLevel()),
                Math.min(100, levelsGained + message.levelsGained()),
                tick,
                ProgressNoticeQueue.durationFor(levelsGained + message.levelsGained()));
    }

    ProgressNotice nextTick() {
        return new ProgressNotice(
                stat, awardedXp, newLevel, levelsGained, updatedAt, remainingTicks - 1);
    }

    public float alpha() {
        if (remainingTicks >= ProgressNoticeQueue.FADE_TICKS) {
            return 1.0F;
        }
        return Math.max(0.0F, remainingTicks / (float) ProgressNoticeQueue.FADE_TICKS);
    }
}
