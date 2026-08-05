package net.ronlab.rgadeathrace.listener;

import com.ronlab.deathrace.DeathRacePlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Alias event listener in net.ronlab.rgadeathrace package extending com.ronlab.deathrace.listener.DeathRaceEventListener.
 */
@NullMarked
public class DeathRaceEventListener extends com.ronlab.deathrace.listener.DeathRaceEventListener {

    public DeathRaceEventListener() {
        super(DeathRacePlugin.getInstance());
    }

    public DeathRaceEventListener(@Nullable DeathRacePlugin plugin) {
        super(plugin);
    }
}
