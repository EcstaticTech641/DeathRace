package com.ronlab.deathrace;

import com.ronlab.deathrace.listener.DeathRaceListener;
import com.ronlab.deathrace.prompt.DeathPrompt;
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

    @Override
    public void onEnable() {
        instance = this;

        // Register event listeners
        getServer().getPluginManager().registerEvents(new DeathRaceListener(this), this);

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
        return activeSessions.remove(worldName);
    }

    public Map<String, DeathRaceSession> getActiveSessions() {
        return Map.copyOf(activeSessions);
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
            if (prompt == null) {
                player.sendMessage(Component.text("No prompt assigned.", NamedTextColor.YELLOW));
                return;
            }

            if (session.isCompleted(player.getUniqueId())) {
                player.sendMessage(Component.text("[DeathRace] You have already completed your prompt: ", NamedTextColor.GREEN)
                        .append(Component.text(prompt.getDescription(), NamedTextColor.WHITE)));
            } else {
                player.sendMessage(Component.text("[DeathRace] Your prompt objective: ", NamedTextColor.GOLD)
                        .append(Component.text(prompt.getDescription(), NamedTextColor.YELLOW)));
            }
        }
    }
}
