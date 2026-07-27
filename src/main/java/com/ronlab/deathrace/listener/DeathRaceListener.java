package com.ronlab.deathrace.listener;

import com.ronlab.deathrace.DeathRacePlugin;
import com.ronlab.deathrace.prompt.DeathPrompt;
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
import java.util.Objects;
import java.util.UUID;

/**
 * Event listener handling DeathRace minigame lifecycle transitions and fatal damage prompt checks.
 */
@NullMarked
public final class DeathRaceListener implements Listener {

    private final DeathRacePlugin plugin;

    public DeathRaceListener(DeathRacePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMinigameStart(MinigameStartEvent event) {
        String worldName = event.getWorldName();
        List<UUID> playerUuids = event.getPlayerUuids();

        DeathRaceSession session = new DeathRaceSession(
                event.getMinigameId(),
                event.getMinigameName(),
                worldName,
                playerUuids
        );

        for (UUID uuid : playerUuids) {
            DeathPrompt prompt = DeathPrompt.getRandomPrompt();
            session.assignPrompt(uuid, prompt);

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                sendPromptNotification(player, prompt);
            }
        }

        plugin.registerSession(worldName, session);
        plugin.getLogger().info("DeathRace session started in world '" + worldName + "' with " + playerUuids.size() + " players.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        String worldName = player.getWorld().getName();
        DeathRaceSession session = plugin.getSession(worldName);
        if (session == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (session.isCompleted(uuid)) {
            return;
        }

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

                // Mark prompt completed
                session.markCompleted(uuid);

                // Send completion feedback
                sendCompletionNotification(player, assignedPrompt);

                // Delegate spectator transition to RGA Session Control API
                RGASessionControl sessionControl = getSessionControl();
                if (sessionControl != null) {
                    sessionControl.setSpectator(player, true);
                }

                // Check session completion trigger
                if (session.isAllCompleted()) {
                    plugin.getLogger().info("All players completed their death prompts in session '" + worldName + "'. Dispatching conclusion.");
                    requestSessionConclude(worldName, "All death race prompts completed", session.getScores());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMinigameConclude(MinigameConcludeEvent event) {
        String worldName = event.getWorldName();
        DeathRaceSession session = plugin.unregisterSession(worldName);
        if (session != null) {
            plugin.getLogger().info("DeathRace session concluded and cleaned up for world '" + worldName + "'.");
        }
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
                plugin.getLogger().warning("Failed to invoke requestSessionConclude on RGA plugin instance: " + e.getMessage());
            }
        }
    }

    private void sendPromptNotification(Player player, DeathPrompt prompt) {
        Component mainTitle = Component.text("DEATH RACE", NamedTextColor.RED, TextDecoration.BOLD);
        Component subTitle = Component.text("Goal: ", NamedTextColor.GRAY)
                .append(Component.text(prompt.getDescription(), NamedTextColor.YELLOW));

        Title title = Title.title(
                mainTitle,
                subTitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500))
        );

        player.showTitle(title);
        player.sendActionBar(subTitle);
        player.sendMessage(Component.text("[DeathRace] Your prompt: ", NamedTextColor.GOLD)
                .append(Component.text(prompt.getDescription(), NamedTextColor.WHITE)));
    }

    private void sendCompletionNotification(Player player, DeathPrompt prompt) {
        Component mainTitle = Component.text("PROMPT COMPLETED!", NamedTextColor.GREEN, TextDecoration.BOLD);
        Component subTitle = Component.text(prompt.getDescription(), NamedTextColor.GRAY);

        Title title = Title.title(
                mainTitle,
                subTitle,
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))
        );

        player.showTitle(title);
        player.sendActionBar(mainTitle);
        player.sendMessage(Component.text("[DeathRace] Successfully completed prompt: ", NamedTextColor.GREEN)
                .append(Component.text(prompt.getDescription(), NamedTextColor.WHITE)));
    }
}
