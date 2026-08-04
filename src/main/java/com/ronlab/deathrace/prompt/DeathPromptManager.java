package com.ronlab.deathrace.prompt;

import com.ronlab.deathrace.DeathRacePlugin;
import com.ronlab.deathrace.session.DeathRaceSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Objective Engine and Action Bar HUD manager for DeathRace sessions.
 */
@NullMarked
public final class DeathPromptManager {

    private final DeathRacePlugin plugin;
    private final ConcurrentHashMap<String, BukkitTask> hudTasks = new ConcurrentHashMap<>();

    public DeathPromptManager(DeathRacePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    public DeathPrompt assignRandomPrompt(DeathRaceSession session, UUID playerUuid) {
        DeathPrompt newPrompt = DeathPrompt.getRandomPrompt();
        session.assignPrompt(playerUuid, newPrompt);
        return newPrompt;
    }

    public void startSessionHud(DeathRaceSession session) {
        String worldName = session.getWorldName();
        stopSessionHud(worldName);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            broadcastSessionHud(session);
        }, 0L, 20L); // Update Action Bar HUD every 1 second

        hudTasks.put(worldName, task);
    }

    public void stopSessionHud(String worldName) {
        BukkitTask existing = hudTasks.remove(worldName);
        if (existing != null && !existing.isCancelled()) {
            existing.cancel();
        }
    }

    public void broadcastSessionHud(DeathRaceSession session) {
        for (UUID uuid : session.getInitialPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updatePlayerHud(player, session);
            }
        }
    }

    public void updatePlayerHud(Player player, DeathRaceSession session) {
        DeathPrompt prompt = session.getAssignedPrompt(player.getUniqueId());
        int score = session.getScore(player.getUniqueId());
        int target = DeathRaceSession.TARGET_SCORE;

        Component hudComponent;
        if (session.hasWinner()) {
            boolean isWinner = Objects.equals(session.getWinnerUuid(), player.getUniqueId());
            if (isWinner) {
                hudComponent = Component.text("VICTORY! Score: " + score + "/" + target, NamedTextColor.GREEN, TextDecoration.BOLD);
            } else {
                hudComponent = Component.text("MATCH CONCLUDED | Score: " + score + "/" + target, NamedTextColor.RED, TextDecoration.BOLD);
            }
        } else if (prompt != null) {
            hudComponent = Component.text("Score: ", NamedTextColor.GOLD)
                    .append(Component.text(score + "/" + target, NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(" | Goal: ", NamedTextColor.GRAY))
                    .append(Component.text(prompt.getDescription(), NamedTextColor.WHITE));
        } else {
            hudComponent = Component.text("DeathRace Active | Score: " + score + "/" + target, NamedTextColor.AQUA);
        }

        player.sendActionBar(hudComponent);
    }
}
