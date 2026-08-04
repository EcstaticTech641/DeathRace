package com.ronlab.deathrace.listener;

import com.ronlab.deathrace.DeathRacePlugin;
import com.ronlab.deathrace.prompt.DeathPrompt;
import com.ronlab.deathrace.prompt.DeathPromptManager;
import com.ronlab.deathrace.session.DeathRaceSession;
import com.ronlab.rga.api.RGASessionControl;
import com.ronlab.rga.api.event.MinigameConcludeEvent;
import com.ronlab.rga.api.event.MinigameStartEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener handling DeathRace minigame lifecycle transitions,
 * First-to-3 score engine, HUD presentation, and fatal damage prompt checks.
 */
@NullMarked
public final class DeathRaceEventListener implements Listener {

    private final @Nullable DeathRacePlugin plugin;

    public DeathRaceEventListener(@Nullable DeathRacePlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isDeathRaceMinigame(@Nullable String minigameId) {
        if (minigameId == null) {
            return false;
        }
        String id = minigameId.toLowerCase().trim();
        if (id.startsWith("rga:")) {
            id = id.substring(4);
        }
        return "deathrace".equals(id) || "death_race".equals(id);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMinigameStart(MinigameStartEvent event) {
        // Enforce strict event payload validation for minigame ID
        if (event == null || !isDeathRaceMinigame(event.getMinigameId())) {
            return; // Ignore events intended for other companion minigames
        }

        String worldName = event.getWorldName();
        List<UUID> playerUuids = event.getPlayerUuids();

        DeathRaceSession session = new DeathRaceSession(
                event.getMinigameId(),
                event.getMinigameName(),
                worldName,
                playerUuids
        );

        DeathPromptManager promptManager = plugin != null ? plugin.getPromptManager() : null;

        // Initialize goal objectives and race HUD for all players
        for (UUID uuid : playerUuids) {
            DeathPrompt prompt = promptManager != null ? promptManager.assignRandomPrompt(session, uuid) : DeathPrompt.getRandomPrompt();
            if (promptManager == null) {
                session.assignPrompt(uuid, prompt);
            }

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Display starting title card, HUD actionbar, and message
                sendStartNotification(player, prompt);
            }
        }

        if (plugin != null) {
            plugin.registerSession(worldName, session);
            if (promptManager != null) {
                promptManager.startSessionHud(session);
            }
            plugin.getLogger().info("DeathRace session started in world '" + worldName + "' with " + playerUuids.size() + " players.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (plugin == null || !(event.getEntity() instanceof Player player)) {
            return;
        }

        String worldName = player.getWorld().getName();
        DeathRaceSession session = plugin.getSession(worldName);
        if (session == null || session.hasWinner()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        DeathPrompt assignedPrompt = session.getAssignedPrompt(uuid);
        if (assignedPrompt == null) {
            return;
        }

        double health = player.getHealth();
        double finalDamage = event.getFinalDamage();

        // Check fatal damage condition ($health - finalDamage <= 0$)
        if (health - finalDamage <= 0.0001) {
            if (assignedPrompt.matches(player, event)) {
                // Cancel damage to prevent standard death screen and item drops
                event.setCancelled(true);

                // Add point to player
                int newScore = session.addPoint(uuid);
                DeathPromptManager promptManager = plugin.getPromptManager();

                // Check win condition (First to 3)
                if (newScore >= DeathRaceSession.TARGET_SCORE) {
                    // Match concluded with winner
                    broadcastMatchVictory(session, player);

                    // Delegate spectator transition to RGA Session Control API for all participants
                    RGASessionControl sessionControl = getSessionControl();
                    if (sessionControl != null) {
                        for (UUID pUuid : session.getInitialPlayers()) {
                            Player p = Bukkit.getPlayer(pUuid);
                            if (p != null && p.isOnline()) {
                                sessionControl.setSpectator(p, true);
                            }
                        }
                    }

                    // Dispatch programmatic session conclusion to RGA Core
                    plugin.getLogger().info("Player '" + player.getName() + "' won DeathRace session in world '" + worldName + "' with score 3/3.");
                    requestSessionConclude(worldName, "Player " + player.getName() + " reached 3 points", session.getScores());
                } else {
                    // Score point awarded; reset player vital stats for next round
                    resetPlayerStatus(player);

                    // Select new random prompt objective for player
                    DeathPrompt nextPrompt = promptManager != null ? promptManager.assignRandomPrompt(session, uuid) : DeathPrompt.getRandomPrompt();

                    // Display completion feedback & new objective
                    sendPointAwardNotification(player, assignedPrompt, nextPrompt, newScore);

                    if (promptManager != null) {
                        promptManager.updatePlayerHud(player, session);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMinigameConclude(MinigameConcludeEvent event) {
        // Enforce strict event payload validation for minigame ID
        if (event == null || !isDeathRaceMinigame(event.getMinigameId())) {
            return; // Ignore events intended for other companion minigames
        }

        String worldName = event.getWorldName();
        if (plugin != null) {
            DeathRaceSession session = plugin.unregisterSession(worldName);
            if (session != null) {
                plugin.getLogger().info("DeathRace session concluded and cleaned up for world '" + worldName + "'.");
            }
        }
    }

    private void resetPlayerStatus(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
    }

    private @Nullable RGASessionControl getSessionControl() {
        Plugin rgaPlugin = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
        if (rgaPlugin instanceof RGASessionControl sessionControl) {
            return sessionControl;
        }
        RegisteredServiceProvider<RGASessionControl> rsp = Bukkit.getServicesManager().getRegistration(RGASessionControl.class);
        return rsp != null ? rsp.getProvider() : null;
    }

    private void requestSessionConclude(String worldName, String reason, Map<UUID, Number> scores) {
        Plugin rgaPlugin = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
        if (rgaPlugin != null) {
            try {
                rgaPlugin.getClass().getMethod("requestSessionConclude", String.class, String.class, Map.class)
                        .invoke(rgaPlugin, worldName, reason, scores);
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().warning("Failed to invoke requestSessionConclude on RGA plugin instance: " + e.getMessage());
                }
            }
        }
    }

    private void sendStartNotification(Player player, DeathPrompt prompt) {
        Component mainTitle = Component.text("DEATH RACE", NamedTextColor.RED, TextDecoration.BOLD);
        Component subTitle = Component.text("First to 3 Points! Goal: ", NamedTextColor.GRAY)
                .append(Component.text(prompt.getDescription(), NamedTextColor.YELLOW));

        Title title = Title.title(
                mainTitle,
                subTitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500))
        );

        player.showTitle(title);
        player.sendActionBar(subTitle);
        player.sendMessage(Component.text("[DeathRace] Match Started! First to 3 wins. Objective: ", NamedTextColor.GOLD)
                .append(Component.text(prompt.getDescription(), NamedTextColor.WHITE)));
    }

    private void sendPointAwardNotification(Player player, DeathPrompt completedPrompt, DeathPrompt nextPrompt, int currentScore) {
        Component mainTitle = Component.text("POINT SCORED! (" + currentScore + "/3)", NamedTextColor.GREEN, TextDecoration.BOLD);
        Component subTitle = Component.text("Next Goal: ", NamedTextColor.GRAY)
                .append(Component.text(nextPrompt.getDescription(), NamedTextColor.YELLOW));

        Title title = Title.title(
                mainTitle,
                subTitle,
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))
        );

        player.showTitle(title);
        player.sendMessage(Component.text("[DeathRace] Point Scored! (" + currentScore + "/3). Next Objective: ", NamedTextColor.GREEN)
                .append(Component.text(nextPrompt.getDescription(), NamedTextColor.WHITE)));
    }

    private void broadcastMatchVictory(DeathRaceSession session, Player winner) {
        Component victorTitle = Component.text("VICTORY!", NamedTextColor.GOLD, TextDecoration.BOLD);
        Component victorSub = Component.text("You won the DeathRace (3/3)!", NamedTextColor.YELLOW);
        Title winnerTitle = Title.title(victorTitle, victorSub, Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(5), Duration.ofSeconds(1)));

        Component defeatedTitle = Component.text("GAME OVER", NamedTextColor.RED, TextDecoration.BOLD);
        Component defeatedSub = Component.text(winner.getName() + " won the DeathRace (3/3)!", NamedTextColor.GRAY);
        Title loserTitle = Title.title(defeatedTitle, defeatedSub, Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(5), Duration.ofSeconds(1)));

        for (UUID uuid : session.getInitialPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                if (p.getUniqueId().equals(winner.getUniqueId())) {
                    p.showTitle(winnerTitle);
                    p.sendMessage(Component.text("[DeathRace] VICTORY! You won the DeathRace with 3 points!", NamedTextColor.GOLD, TextDecoration.BOLD));
                } else {
                    p.showTitle(loserTitle);
                    p.sendMessage(Component.text("[DeathRace] Game Over! " + winner.getName() + " won the DeathRace with 3 points.", NamedTextColor.RED));
                }
            }
        }
    }
}
