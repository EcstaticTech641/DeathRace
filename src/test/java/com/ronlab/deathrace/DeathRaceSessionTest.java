package com.ronlab.deathrace;

import com.ronlab.deathrace.prompt.DeathPrompt;
import com.ronlab.deathrace.session.DeathRaceSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeathRaceSessionTest {

    @Test
    @DisplayName("Session initializes players and prompt assignments correctly")
    void testSessionInitialization() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        List<UUID> players = List.of(player1, player2);

        DeathRaceSession session = new DeathRaceSession("deathrace", "Death Race", "session_world_1", players);

        assertEquals("deathrace", session.getMinigameId());
        assertEquals("Death Race", session.getMinigameName());
        assertEquals("session_world_1", session.getWorldName());
        assertEquals(2, session.getInitialPlayers().size());

        session.assignPrompt(player1, DeathPrompt.ANVIL);
        session.assignPrompt(player2, DeathPrompt.LAVA);

        assertEquals(DeathPrompt.ANVIL, session.getAssignedPrompt(player1));
        assertEquals(DeathPrompt.LAVA, session.getAssignedPrompt(player2));
        assertFalse(session.isAllCompleted());
    }

    @Test
    @DisplayName("Prompt completion and score calculation works immutably")
    void testPromptCompletionAndScores() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        List<UUID> players = List.of(player1, player2);

        DeathRaceSession session = new DeathRaceSession("deathrace", "Death Race", "session_world_2", players);
        session.assignPrompt(player1, DeathPrompt.HIT_GROUND_TOO_HARD);
        session.assignPrompt(player2, DeathPrompt.CREEPER_EXPLOSION);

        assertTrue(session.markCompleted(player1));
        assertTrue(session.isCompleted(player1));
        assertFalse(session.isCompleted(player2));
        assertFalse(session.isAllCompleted());

        assertTrue(session.markCompleted(player2));
        assertTrue(session.isAllCompleted());

        Map<UUID, Number> scores = session.getScores();
        assertEquals(1, scores.get(player1));
        assertEquals(1, scores.get(player2));
    }
}
