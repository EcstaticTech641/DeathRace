# DeathRace Companion Plugin

A PaperMC companion minigame plugin for **Ronlab Game Assistant (RGA)** that translates the mechanics of the Scommander DeathRace datapack into a high-performance Java minigame engine.

---

## Overview

**What It Does**
DeathRace is a competitive minigame where players race to complete randomly assigned death objectives (e.g., drowning, pricked by cactus, squashed by anvil, blown up by creeper, hit ground too hard). The first player to complete **3 death objectives** wins the match!

**Key Features:**
- **Death Race Objective Engine:** 35+ translated death causes mapped to native Bukkit `DamageCause` and `Entity` predicate matchers.
- **First-to-3 Point System:** Tracks score per player, resets vital stats upon scoring, and concludes match at 3 points.
- **Real-Time Action Bar HUD:** Displays live player scores and objective descriptions (e.g., `Score: 1/3 | Goal: [player] drowned`).
- **RGA API Integration:** Responds to `MinigameStartEvent`, manages spectators via `RGASessionControl`, and dispatches `requestSessionConclude` for world teardown and inventory restoration.
- **Zero Legacy Interference:** 0 legacy `GameMode.SPECTATOR` or `getInventory().clear()` calls; 100% managed through RGA Core contracts.

---

## Repository Structure & Organization

```
M:\projects\deathrace\
├── pom.xml                                   → Maven project object model (Java 25)
├── README.md                                 → Setup guide, documentation, & architecture overview
├── .gitignore                                → Standard Git exclusion rules
├── src/
│   ├── main/
│   │   ├── java/com/ronlab/deathrace/
│   │   │   ├── DeathRacePlugin.java         → Main JavaPlugin entry point
│   │   │   ├── listener/
│   │   │   │   └── DeathRaceEventListener.java → Minigame & damage event handlers
│   │   │   ├── prompt/
│   │   │   │   ├── DeathPrompt.java         → Enum registry of 35+ death objectives
│   │   │   │   └── DeathPromptManager.java  → Objective engine & Action Bar HUD presenter
│   │   │   └── session/
│   │   │       └── DeathRaceSession.java    → Thread-safe session & score model
│   │   └── resources/
│   │       └── plugin.yml                   → Plugin metadata (load: BEFORE, depend: RonlabGameAssistant)
│   └── test/
│       └── java/com/ronlab/deathrace/
│           ├── DeathRaceEventListenerTest.java → Unit tests for minigame ID filtering
│           └── DeathRaceSessionTest.java      → Unit tests for First-to-3 score engine
```

---

## Quick Start & Setup Guide

### Prerequisites
- **Java JDK 25** (or compatible release)
- **Apache Maven 3.8+**
- **PaperMC Server 26.2+** (1.21.4 API baseline)
- **RonlabGameAssistant (`rga-api:1.13.0-SNAPSHOT`)** installed in local Maven repository or server

### Building from Source

```bash
cd M:\projects\deathrace
mvn clean package
```

The compiled plugin JAR will be produced at:
`target/deathrace-1.0.0-SNAPSHOT.jar`

### Server Deployment

1. Build `RonlabGameAssistant` (`M:\projects\ronlabgameassistant`) and install `rga-api` to `.m2`.
2. Copy `RonlabGameAssistant-1.13.0-SNAPSHOT.jar` and `deathrace-1.0.0-SNAPSHOT.jar` to your Paper server's `plugins/` directory.
3. Start or reload your Paper server.
4. Players can join a DeathRace minigame via the RGA lobby GUI or `/rga` menu!

### Commands

- `/deathprompt` (Aliases: `/prompt`, `/drprompt`) — Check your current assigned death objective and score.

---

## Versioning & Compatibility

- **Plugin Version:** `1.0.0-SNAPSHOT` (Semantic Versioning 2.0.0 compliant)
- **Target Java Release:** 25
- **Paper API:** `1.21.4-R0.1-SNAPSHOT` / `26.2`
- **RGA API:** `1.13.0-SNAPSHOT`
