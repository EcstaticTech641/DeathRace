# DeathRace Companion Plugin — User & Developer Guide

Welcome to the **DeathRace Companion Plugin (`rga-deathrace`)** User & Developer Guide. This plugin provides a high-performance Java implementation of the DeathRace minigame for **PaperMC 26.2 (Java 25)**, operating within the **Ronlab Micro-Companion Architecture (CPMK)** framework.

---

## Table of Contents

1. [Minigame Mechanics & Gameplay Rules](#minigame-mechanics--gameplay-rules)
2. [Commands & Permission Nodes](#commands--permission-nodes)
3. [CPMK Event Bus Integration](#cpmk-event-bus-integration)
4. [Solo QA Developer Mode Guide](#solo-qa-developer-mode-guide)
5. [Configuration & Arena Metadata Reference](#configuration--arena-metadata-reference)
6. [Scoreboard & UI Specification](#scoreboard--ui-specification)

---

## Minigame Mechanics & Gameplay Rules

DeathRace is a competitive race where players attempt to complete randomly assigned death objectives as fast as possible.

### Key Rulesets:
- **First-to-3 Victory:** The first player to complete 3 death prompt objectives wins the match.
- **Objective Assignment:** On match start and after every point scored, players are assigned a random objective from a pool of 35+ death causes (e.g. drowning, cactus prick, anvil squash, creeper explosion, kinetic fall).
- **Fatal Damage Interception:**
  - Death prompts monitor incoming fatal damage (`health - finalDamage <= 0`).
  - If the damage type matches the assigned prompt, the damage event is cancelled (`event.setCancelled(true)`), preventing normal death screens or item drops.
  - The player's score increases by 1, health is restored to full (20.0 HP), food level is reset, fire ticks are cleared, and a new prompt objective is assigned immediately.
- **Match Conclusion:** Reaching 3 points triggers victory broadcasts, transitions participants to spectator mode via `RGASessionControl`, and dispatches `requestSessionConclude` to `rga-core`.

---

## Commands & Permission Nodes

### Player Commands

#### `/deathprompt`
- **Aliases:** `/prompt`, `/drprompt`
- **Description:** Displays the executing player's current active death objective and current score (e.g., `Score: 1/3 | Objective: Drown in water`).
- **Permission Node:** `deathrace.command.prompt` (Default: `true` for all players)
- **Usage:** Run in-game during an active DeathRace session.

### Administrative Permissions

- **`deathrace.admin`:** Grants access to administrative debug and session inspection commands.

---

## CPMK Event Bus Integration

The DeathRace companion plugin operates strictly on the `rga-api` CPMK event bus.

### 1. Match Initialization (`MinigameStartEvent`)
- **Event Priority:** `NORMAL`
- **Handler:** `DeathRaceEventListener.onMinigameStart`
- **Workflow:**
  1. Validates `minigameId` matching `"deathrace"` or `"rga:deathrace"`.
  2. Constructs a `DeathRaceSession` instance bound to the match world name.
  3. Assigns starting `DeathPrompt` objectives to all participating player UUIDs.
  4. Triggers start notifications (title card, action bar HUD, chat message).
  5. Schedules post-teleport scoreboard attachment (delayed by 2 ticks to ensure chunk loading completion).
  6. Starts the 1-second Action Bar & Scoreboard update ticker.

### 2. Match Conclusion (`MinigameConcludeEvent`)
- **Event Priority:** `MONITOR`
- **Handler:** `DeathRaceEventListener.onMinigameConclude`
- **Workflow:**
  1. Validates `minigameId` matching `"deathrace"`.
  2. Cancels the active HUD ticker task for the world.
  3. Restores each participant's scoreboard to `Bukkit.getScoreboardManager().getMainScoreboard()`.
  4. Unregisters custom objective instances to clear sidebar rendering.
  5. Unregisters and removes the `DeathRaceSession` instance.

---

## Solo QA Developer Mode Guide

Solo QA Developer Mode enables single-player testing of arena geometry, fall thresholds, prompt predicates, and scoreboard rendering.

### Activation:
- Solo QA Mode is automatically active whenever a session starts with `initialPlayerCount == 1`.

### Behavior & Features:
- **Win Condition Freeze:** Single-player sessions will not auto-conclude or fail on start due to missing opponents.
- **Full Point Lifecycle:** The QA developer can earn points 1, 2, and 3 sequentially, verifying prompt execution and reset logic.
- **Continuous Map Testing:** QA testers can test fall thresholds (`fall-threshold-y: -64.0`) and pedestal coordinates repeatedly without match aborts.
- **Clean Teardown:** Upon earning 3 points or concluding the session, all scoreboards and HUD elements tear down cleanly.

---

## Configuration & Arena Metadata Reference

### `config.yml` Parameters

```yaml
minigame:
  id: "deathrace"             # CPMK minigame identifier
  name: "Death Race"          # Display name for UI and scoreboards
  target-score: 3             # Points required for victory

match:
  time-limit-seconds: 600     # Maximum round duration (seconds)
  fall-threshold-y: -64.0     # World Y threshold for void resets
  hud-update-interval-ticks: 20 # HUD update rate in server ticks

default-vectors:
  spawn: { x: 0.5, y: 65.0, z: 0.5, yaw: 0.0, pitch: 0.0 }
  pedestal: { x: 0.5, y: 66.0, z: 0.5 }

prompts:
  random-assignment: true
  total-registered-prompts: 35
```

### `arena.yml` Parameters

```yaml
arena:
  id: "deathrace_default"
  name: "DeathRace Classic Arena"
  world: "world_deathrace"
  fall-threshold-y: -64.0
  time-limit-seconds: 600
  spawn-vectors:
    - { slot: 1, x: 0.5, y: 65.0, z: 0.5, yaw: 0.0, pitch: 0.0 }
    - { slot: 2, x: 10.5, y: 65.0, z: 0.5, yaw: 90.0, pitch: 0.0 }
  pedestals:
    first-place: { x: 0.5, y: 68.0, z: -15.5, yaw: 0.0, pitch: 0.0 }
    second-place: { x: -2.5, y: 67.0, z: -15.5, yaw: 0.0, pitch: 0.0 }
    third-place: { x: 3.5, y: 66.0, z: -15.5, yaw: 0.0, pitch: 0.0 }
```

---

## Scoreboard & UI Specification

- **Blank Margin Numbering:** In accordance with PaperMC 26.2 standards, all sidebar lines utilize `objective.numberFormat(NumberFormat.blank())` and `score.numberFormat(NumberFormat.blank())` to remove standard red margin scores.
- **Post-Teleport Delay:** Scoreboards are attached 2 ticks post-teleport to prevent client chunk-loading hangs.
- **Sidebar Display Layout:**
  ```text
    DEATH RACE  
  -----------------
  Goal: [Active Objective]
  Points: 0/3 pts
  Time: 01:23
  -----------------
  ```
