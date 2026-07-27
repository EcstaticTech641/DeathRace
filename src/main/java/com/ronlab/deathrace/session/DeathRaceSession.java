package com.ronlab.deathrace.session;

import com.ronlab.deathrace.prompt.DeathPrompt;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulates the active state and prompt assignments for a single DeathRace minigame session.
 */
@NullMarked
public final class DeathRaceSession {

    private final String minigameId;
    private final String minigameName;
    private final String worldName;
    private final List<UUID> initialPlayers;
    private final ConcurrentHashMap<UUID, DeathPrompt> playerPrompts = new ConcurrentHashMap<>();
    private final Set<UUID> completedPlayers = ConcurrentHashMap.newKeySet();

    public DeathRaceSession(String minigameId, String minigameName, String worldName, List<UUID> playerUuids) {
        this.minigameId = Objects.requireNonNull(minigameId, "minigameId cannot be null");
        this.minigameName = Objects.requireNonNull(minigameName, "minigameName cannot be null");
        this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
        this.initialPlayers = playerUuids != null ? List.copyOf(playerUuids) : Collections.emptyList();
    }

    public String getMinigameId() {
        return minigameId;
    }

    public String getMinigameName() {
        return minigameName;
    }

    public String getWorldName() {
        return worldName;
    }

    public List<UUID> getInitialPlayers() {
        return initialPlayers;
    }

    public void assignPrompt(UUID playerUuid, DeathPrompt prompt) {
        playerPrompts.put(playerUuid, prompt);
    }

    public @Nullable DeathPrompt getAssignedPrompt(UUID playerUuid) {
        return playerPrompts.get(playerUuid);
    }

    public boolean markCompleted(UUID playerUuid) {
        return completedPlayers.add(playerUuid);
    }

    public boolean isCompleted(UUID playerUuid) {
        return completedPlayers.contains(playerUuid);
    }

    public boolean isAllCompleted() {
        if (playerPrompts.isEmpty()) {
            return false;
        }
        return completedPlayers.containsAll(playerPrompts.keySet());
    }

    public Map<UUID, DeathPrompt> getPromptAssignments() {
        return Map.copyOf(playerPrompts);
    }

    public Set<UUID> getCompletedPlayers() {
        return Set.copyOf(completedPlayers);
    }

    public Map<UUID, Number> getScores() {
        ConcurrentHashMap<UUID, Number> scores = new ConcurrentHashMap<>();
        for (UUID uuid : initialPlayers) {
            scores.put(uuid, completedPlayers.contains(uuid) ? 1 : 0);
        }
        return Map.copyOf(scores);
    }
}
