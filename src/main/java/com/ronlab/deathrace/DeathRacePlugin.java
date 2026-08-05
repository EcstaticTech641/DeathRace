package com.ronlab.deathrace;

import com.ronlab.deathrace.listener.DeathRaceEventListener;
import com.ronlab.deathrace.prompt.DeathPrompt;
import com.ronlab.deathrace.prompt.DeathPromptManager;
import com.ronlab.deathrace.session.DeathRaceSession;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main plugin entry point for DeathRace companion plugin.
 */
@NullMarked
public final class DeathRacePlugin extends JavaPlugin {

    private static @Nullable DeathRacePlugin instance;
    private final ConcurrentHashMap<String, DeathRaceSession> activeSessions = new ConcurrentHashMap<>();
    private @Nullable DeathPromptManager promptManager;

    @Override
    public void onLoad() {
        // Set singleton early in the Paper lifecycle so getInstance() is safe
        // before any companion plugin's onEnable() runs.
        instance = this;
    }

    @Override
    public void onEnable() {
        instance = this; // Redundant but explicit — guards against future refactors.
        promptManager = new DeathPromptManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new DeathRaceEventListener(this), this);

        // Register paper BasicCommand for prompt inspection
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                    "deathprompt",
                    "Check your assigned DeathRace prompt objective",
                    List.of("prompt", "drprompt"),
                    new DeathPromptCommand()
            );
        });

        getLogger().info("DeathRace companion plugin enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (promptManager != null) {
            for (String worldName : activeSessions.keySet()) {
                promptManager.stopSessionHud(worldName);
            }
        }
        activeSessions.clear();
        instance = null;
        getLogger().info("DeathRace companion plugin disabled.");
    }

    public void registerSession(String worldName, DeathRaceSession session) {
        Objects.requireNonNull(worldName, "worldName cannot be null");
        Objects.requireNonNull(session, "session cannot be null");
        activeSessions.put(worldName, session);
    }

    public @Nullable DeathRaceSession getSession(@Nullable String worldName) {
        if (worldName == null) return null;
        return activeSessions.get(worldName);
    }

    public @Nullable DeathRaceSession unregisterSession(@Nullable String worldName) {
        if (worldName == null) return null;
        DeathRaceSession session = activeSessions.remove(worldName);
        if (promptManager != null) {
            if (session != null) {
                // Session-aware teardown: cancels the HUD ticker and destroys
                // each player's scoreboard sidebar so the client is cleared.
                promptManager.stopSessionHud(session);
            } else {
                // Fallback: session was already removed or never registered;
                // at minimum cancel the HUD ticker if one is still running.
                promptManager.stopSessionHud(worldName);
            }
        }
        return session;
    }

    public Map<String, DeathRaceSession> getActiveSessions() {
        return Map.copyOf(activeSessions);
    }

    public @Nullable DeathPromptManager getPromptManager() {
        return promptManager;
    }

    public static @Nullable DeathRacePlugin getInstance() {
        return instance;
    }

    private final class DeathPromptCommand implements BasicCommand {
        @Override
        public void execute(CommandSourceStack sourceStack, String[] args) {
            if (!(sourceStack.getExecutor() instanceof Player player)) {
                sourceStack.getSender().sendMessage(Component.text("This command can only be executed by players.", NamedTextColor.RED));
                return;
            }

            DeathRaceSession session = getSession(player.getWorld().getName());
            if (session == null) {
                player.sendMessage(Component.text("You are not currently in an active DeathRace session.", NamedTextColor.YELLOW));
                return;
            }

            DeathPrompt prompt = session.getAssignedPrompt(player.getUniqueId());
            int score = session.getScore(player.getUniqueId());
            if (prompt == null) {
                player.sendMessage(Component.text("No prompt assigned.", NamedTextColor.YELLOW));
                return;
            }

            if (session.hasWinner()) {
                player.sendMessage(Component.text("[DeathRace] Match concluded! Your final score: " + score + "/" + DeathRaceSession.TARGET_SCORE, NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("[DeathRace] Score: " + score + "/" + DeathRaceSession.TARGET_SCORE + " | Objective: ", NamedTextColor.GOLD)
                        .append(Component.text(prompt.getDescription(), NamedTextColor.YELLOW)));
            }
        }
    }
}
