package com.ronlab.deathrace.ui;

import com.ronlab.deathrace.prompt.DeathPrompt;
import com.ronlab.deathrace.session.DeathRaceSession;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

/**
 * Modern Paper 26.2 Scoreboard wrapper for DeathRace displaying active objective,
 * leader points, and match duration with blank number formatting.
 */
@NullMarked
public final class DeathRaceScoreboard {

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Score goalLine;
    private final Score pointsLine;
    private final Score timeLine;

    public DeathRaceScoreboard() {
        this.scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager(), "ScoreboardManager cannot be null").getNewScoreboard();
        this.objective = scoreboard.registerNewObjective(
                "deathrace",
                Criteria.DUMMY,
                Component.text("  DEATH RACE  ", NamedTextColor.GOLD, TextDecoration.BOLD)
        );
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective.numberFormat(NumberFormat.blank());

        Score headerLine = objective.getScore("line_header");
        headerLine.customName(Component.text("-----------------", NamedTextColor.DARK_GRAY));
        headerLine.numberFormat(NumberFormat.blank());

        this.goalLine = objective.getScore("line_goal");
        this.goalLine.customName(Component.text("Goal: ", NamedTextColor.GRAY).append(Component.text("None", NamedTextColor.YELLOW)));
        this.goalLine.numberFormat(NumberFormat.blank());

        this.pointsLine = objective.getScore("line_points");
        this.pointsLine.customName(Component.text("Points: ", NamedTextColor.GRAY).append(Component.text("0/3 pts", NamedTextColor.GREEN)));
        this.pointsLine.numberFormat(NumberFormat.blank());

        this.timeLine = objective.getScore("line_time");
        this.timeLine.customName(Component.text("Time: ", NamedTextColor.GRAY).append(Component.text("00:00", NamedTextColor.WHITE)));
        this.timeLine.numberFormat(NumberFormat.blank());

        Score footerLine = objective.getScore("line_footer");
        footerLine.customName(Component.text("-----------------", NamedTextColor.DARK_GRAY));
        footerLine.numberFormat(NumberFormat.blank());
    }

    public void update(Player player, DeathRaceSession session, long elapsedSeconds) {
        DeathPrompt prompt = session.getAssignedPrompt(player.getUniqueId());
        int score = session.getScore(player.getUniqueId());

        String goalText = prompt != null ? prompt.getDescription() : "None";
        if (goalText.length() > 28) {
            goalText = goalText.substring(0, 25) + "...";
        }

        goalLine.customName(Component.text("Goal: ", NamedTextColor.GRAY)
                .append(Component.text(goalText, NamedTextColor.YELLOW)));

        pointsLine.customName(Component.text("Points: ", NamedTextColor.GRAY)
                .append(Component.text(score + "/" + DeathRaceSession.TARGET_SCORE + " pts", NamedTextColor.GREEN, TextDecoration.BOLD)));

        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        timeLine.customName(Component.text("Time: ", NamedTextColor.GRAY)
                .append(Component.text(timeStr, NamedTextColor.WHITE)));
    }

    public void attach(Player player) {
        player.setScoreboard(scoreboard);
    }

    /**
     * Detaches this scoreboard from the given player and unregisters the
     * underlying objective so the sidebar is cleared immediately on the client.
     * Safe to call even if the player is offline or the objective is already
     * unregistered.
     *
     * @param player the player to detach from
     */
    public void destroy(Player player) {
        if (player.isOnline()) {
            // Restore the server main scoreboard — clears the Death Race sidebar
            player.setScoreboard(
                    Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard()
            );
        }
        try {
            objective.unregister();
        } catch (IllegalStateException ignored) {
            // Already unregistered — safe to ignore
        }
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }
}
