package com.ronlab.deathrace.prompt;

import com.ronlab.deathrace.DeathRacePlugin;
import com.ronlab.deathrace.session.DeathRaceSession;
import com.ronlab.deathrace.ui.DeathRaceScoreboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Objective Engine and Action Bar HUD / Scoreboard manager for DeathRace sessions.
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

    public void bindPostTeleportScoreboard(Player player, DeathRaceSession session) {
        // Attach scoreboard during post-teleport spawn phase (2 ticks later after chunk load)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                DeathRaceScoreboard sb = session.getOrCreateScoreboard(player.getUniqueId());
                sb.update(player, session, session.getElapsedSeconds());
                sb.attach(player);
            }
        }, 2L);
    }

    public void startSessionHud(DeathRaceSession session) {
        String worldName = session.getWorldName();
        stopSessionHud(worldName);

        // Schedule post-teleport scoreboard binding for all initial players
        for (UUID uuid : session.getInitialPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                bindPostTeleportScoreboard(p, session);
            }
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            broadcastSessionHud(session);
        }, 0L, 20L); // Update Action Bar HUD and Scoreboard every 1 second

        hudTasks.put(worldName, task);
    }

    public void stopSessionHud(String worldName) {
        BukkitTask existing = hudTasks.remove(worldName);
        if (existing != null && !existing.isCancelled()) {
            existing.cancel();
        }
    }

    /**
     * Stops the HUD ticker and destroys each player's scoreboard sidebar for
     * the given session. This is the preferred teardown entry point when the
     * session object is still available; it ensures the Death Race sidebar is
     * cleared from every participant's client immediately.
     *
     * @param session the session whose scoreboards should be torn down
     */
    public void stopSessionHud(DeathRaceSession session) {
        // Cancel the repeating HUD ticker first
        stopSessionHud(session.getWorldName());

        // Restore each player's scoreboard to the server default
        for (UUID uuid : session.getInitialPlayers()) {
            DeathRaceScoreboard sb = session.getScoreboard(uuid);
            if (sb == null) continue;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                sb.destroy(player);
            }
        }
    }

    public void broadcastSessionHud(DeathRaceSession session) {
        long elapsed = session.getElapsedSeconds();
        for (UUID uuid : session.getInitialPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updatePlayerHud(player, session);
                DeathRaceScoreboard sb = session.getOrCreateScoreboard(uuid);
                sb.update(player, session, elapsed);
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
