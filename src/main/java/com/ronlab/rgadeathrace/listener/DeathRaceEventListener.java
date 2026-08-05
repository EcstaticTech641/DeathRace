package com.ronlab.rgadeathrace.listener;

import com.ronlab.deathrace.DeathRacePlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Event listener wrapper in com.ronlab.rgadeathrace.listener package extending com.ronlab.deathrace.listener.DeathRaceEventListener.
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
