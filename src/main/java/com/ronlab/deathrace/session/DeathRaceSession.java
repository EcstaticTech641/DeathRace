package com.ronlab.deathrace.session;

import com.ronlab.deathrace.prompt.DeathPrompt;
import com.ronlab.deathrace.ui.DeathRaceScoreboard;
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
 * Encapsulates the active state, First-to-3 score tracking, match timer,
 * and prompt objectives for a single DeathRace minigame session.
 */
@NullMarked
public final class DeathRaceSession {

    public static final int TARGET_SCORE = 3;

    private final String minigameId;
    private final String minigameName;
    private final String worldName;
    private final List<UUID> initialPlayers;
    private final long startTimeMillis;
    private final ConcurrentHashMap<UUID, DeathPrompt> playerPrompts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> playerScores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, DeathRaceScoreboard> playerScoreboards = new ConcurrentHashMap<>();
    private final Set<UUID> completedPlayers = ConcurrentHashMap.newKeySet();
    private volatile @Nullable UUID winnerUuid;

    public DeathRaceSession(String minigameId, String minigameName, String worldName, List<UUID> playerUuids) {
        this.minigameId = Objects.requireNonNull(minigameId, "minigameId cannot be null");
        this.minigameName = Objects.requireNonNull(minigameName, "minigameName cannot be null");
        this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
        this.initialPlayers = playerUuids != null ? List.copyOf(playerUuids) : Collections.emptyList();
        this.startTimeMillis = System.currentTimeMillis();
        for (UUID uuid : initialPlayers) {
            playerScores.put(uuid, 0);
        }
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

    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000L;
    }

    public boolean isSoloQaMode() {
        return initialPlayers.size() == 1;
    }

    public void assignPrompt(UUID playerUuid, DeathPrompt prompt) {
        playerPrompts.put(playerUuid, prompt);
    }

    public @Nullable DeathPrompt getAssignedPrompt(UUID playerUuid) {
        return playerPrompts.get(playerUuid);
    }

    public int getScore(UUID playerUuid) {
        return playerScores.getOrDefault(playerUuid, 0);
    }

    public int addPoint(UUID playerUuid) {
        int newScore = playerScores.compute(playerUuid, (k, current) -> (current == null ? 0 : current) + 1);
        if (newScore >= TARGET_SCORE && winnerUuid == null) {
            winnerUuid = playerUuid;
            completedPlayers.add(playerUuid);
        }
        return newScore;
    }

    public boolean isCompleted(UUID playerUuid) {
        return completedPlayers.contains(playerUuid) || (winnerUuid != null && winnerUuid.equals(playerUuid));
    }

    public boolean isAllCompleted() {
        if (playerPrompts.isEmpty()) {
            return false;
        }
        if (isSoloQaMode()) {
            // Solo QA Developer Mode: suppress instant victory on start; require 3 points to win
            return winnerUuid != null;
        }
        return completedPlayers.containsAll(playerPrompts.keySet());
    }

    public boolean hasWinner() {
        return winnerUuid != null;
    }

    public @Nullable UUID getWinnerUuid() {
        return winnerUuid;
    }

    public DeathRaceScoreboard getOrCreateScoreboard(UUID playerUuid) {
        return playerScoreboards.computeIfAbsent(playerUuid, k -> new DeathRaceScoreboard());
    }

    /**
     * Returns the scoreboard for the given player if one has already been
     * created, or {@code null} if the player never had one attached.
     * Unlike {@link #getOrCreateScoreboard}, this method will never
     * instantiate a new scoreboard — safe to call during teardown.
     */
    public @Nullable DeathRaceScoreboard getScoreboard(UUID playerUuid) {
        return playerScoreboards.get(playerUuid);
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
            scores.put(uuid, playerScores.getOrDefault(uuid, 0));
        }
        return Map.copyOf(scores);
    }
}
