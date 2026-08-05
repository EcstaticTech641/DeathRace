package net.ronlab.rgadeathrace;

import com.ronlab.deathrace.DeathRacePlugin;
import com.ronlab.deathrace.listener.DeathRaceEventListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Companion entry point in net.ronlab.rgadeathrace package registering DeathRaceEventListener with Bukkit's PluginManager.
 */
@NullMarked
public class DeathRaceCompanion extends JavaPlugin {

    private static @Nullable DeathRaceCompanion instance;

    @Override
    public void onEnable() {
        instance = this;

        // Explicitly register DeathRaceEventListener with Bukkit's PluginManager.
        // No-arg constructor resolves DeathRacePlugin via getInstance() with a
        // JavaPlugin.getPlugin() fallback, guarding against initialization order races.
        getServer().getPluginManager().registerEvents(new DeathRaceEventListener(), this);

        getLogger().info("[DeathRace HANDSHAKE] DeathRaceCompanion registered DeathRaceEventListener explicitly.");
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static @Nullable DeathRaceCompanion getInstance() {
        return instance;
    }
}
