# DeathRace Companion Plugin (CPMK Integration)

A high-performance PaperMC companion minigame plugin for **Ronlab Game Assistant (RGA)** built under the **Micro-Companion Architecture (CPMK)** specification.

---

## Technical Overview & Environment

- **Plugin Name:** `rga-deathrace` (`DeathRace`)
- **Version:** `1.0.0-SNAPSHOT`
- **Target JDK:** Java 25
- **Server Environment:** PaperMC 26.2 (1.21.4 API baseline)
- **Framework API:** `com.ronlab:rga-api:1.13.0-SNAPSHOT`
- **Dependency Specification:** `dependencies.server.RonlabGameAssistant` (`required: true`, `join-classpath: true`)

---

## 5 CPMK Architectural Pillars Compliance

1. **Core Gameplay Function Retention:**
   - 100% preservation of native DeathRace minigame mechanics translated from the Scommander DeathRace datapack.
   - Features a First-to-3 point score engine, 35+ death prompt objective matchers, local scoreboards, and real-time Action Bar HUD tickers.

2. **Ronlab Integration Standard:**
   - Listens strictly for CPMK event bus payloads: `MinigameStartEvent` and `MinigameConcludeEvent`.
   - Plugin descriptors (`paper-plugin.yml` and `plugin.yml`) enforce `api-version: '26.2'` and list `RonlabGameAssistant` with `required: true` and `join-classpath: true` without invalid `load: BEFORE` directives.

3. **Baseline Structure & Rules Provision:**
   - Uses PaperMC's `objective.numberFormat(NumberFormat.blank())` and `Score.numberFormat(NumberFormat.blank())` across all sidebar lines to eliminate margin red numbers.
   - Enforces post-teleport scoreboard attachment (`player.setScoreboard()`) delayed by 2 ticks after spawn to prevent chunk-loading hangs.
   - Guarantees teardown cleanup by restoring `player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard())` and unregistering objectives upon `MinigameConcludeEvent`.

4. **Companion-Type Agnostic Design:**
   - Operates as a decoupled, self-contained module communicating solely through the `rga-api` event bus and Java reflection service providers.

5. **Feature Implementation & Modification Specs:**
   - Includes local command `/deathprompt` (`/prompt`, `/drprompt`).
   - Implements **Solo QA Developer Mode** (`initialPlayerCount == 1`) to freeze instant victory conditions and allow continuous single-player testing of map resets, fall zones, and death objectives.

---

## Minigame Mechanics & Objective Rules

- **First-to-3 Objective Engine:** Players are assigned a random death objective (e.g., drowning, pricked by cactus, blown up by creeper, hit ground too hard).
- **Fatal Damage Interception:** When a player suffers fatal damage (`health - finalDamage <= 0`) matching their prompt, the damage event is cancelled (preventing death screen and item loss), 1 point is awarded, health/vital stats are restored, and a new objective is assigned.
- **Match Victory:** The first player to reach 3 points triggers match conclusion, broadcasts victory titles, transitions participants to spectator mode via `RGASessionControl`, and dispatches `requestSessionConclude` to `rga-core`.

---

## Solo QA Developer Mode (`initialPlayerCount == 1`)

When a DeathRace session is launched with a single player (`initialPlayerCount == 1`):
- **Win Condition Freeze:** Instant win or forfeit checks are suppressed.
- **Continuous Testing:** The session remains active until 3 points are earned, allowing QA developers to repeatedly test fall thresholds, pedestal spawns, death prompt predicates, Action Bar HUD updates, and scoreboard sidebar rendering in isolation.

---

## CPMK Event Bus Integration

- **`MinigameStartEvent` (`EventPriority.NORMAL`):** Constructs a `DeathRaceSession`, assigns initial death prompts, displays title cards, registers the session, and starts the 1-second HUD/scoreboard update task (`DeathPromptManager`).
- **`MinigameConcludeEvent` (`EventPriority.MONITOR`):** Cancels the HUD task, detaches custom scoreboards, restores main server scoreboards, unregisters objectives, and cleans up active session state.

---

## Commands & Permissions

| Command | Aliases | Description | Permission Node |
| :--- | :--- | :--- | :--- |
| `/deathprompt` | `/prompt`, `/drprompt` | Inspect your current assigned death objective and score | `deathrace.command.prompt` |

---

## Configuration & Arena Files

- **`src/main/resources/config.yml`:** Default plugin settings, target score (3), time limit (600s), fall threshold (-64.0 Y), and HUD update intervals.
- **`src/main/resources/arena.yml`:** Arena metadata, spawn vectors, podium/pedestal coordinates, and fall zone settings.

---

## Quick Start & Build Instructions

```bash
# Build the companion plugin JAR
mvn clean package
```

Output artifact: `target/deathrace-1.0.0-SNAPSHOT.jar`
