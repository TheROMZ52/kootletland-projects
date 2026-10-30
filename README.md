# KootletLand Projects

A toolbox for the KootletLand universe — anti-cheat, server utilities, and fewer “who broke the server?” moments.

A collection of Minecraft server tools and development projects for the KootletLand network.

## Projects

### KootletLandAC

An evidence-driven anti-cheat plugin for Paper 1.21.x, built with Java 21. It correlates signals over time instead of issuing a punishment from a single threshold.

- Confidence-based evidence and cross-check correlation
- Movement and combat checks
- Optional GrimAC signal integration
- Optional SQLite/MySQL evidence storage
- Optional Velocity bridge for cross-server staff alerts
- Automatic punishment disabled by default

**Current documented version:** 1.2.0.

**Important:** the plugin has not yet been validated on a live server with real players. Treat detections as staff-review leads, not proof. See the full [KootletLandAC documentation](KootletLandAC/README.md).

### KootletClient

A PvP client mod for KootletLand on Minecraft 1.8.9 (Forge). HUD modules, keystrokes, target HUD, custom crosshair, smooth zoom, an in-game settings menu with themes, and an animated KootletLand main menu. Only legit modules, no cheats.

See the [KootletClient documentation](KootletClient/README.md).

## Repository structure

- `KootletLandAC/` — Paper anti-cheat plugin and Velocity alert module
- `KootletClient/` — Forge 1.8.9 PvP client mod

## Build

From the plugin directory:

```bash
cd KootletLandAC
gradle test build
```

Requirements: Java 21 and Paper 1.21.x for a server installation.

## Documentation

- [English](README.md)
- [فارسی](README.fa.md)
- [KootletLandAC technical documentation](KootletLandAC/README.md)
- [KootletLandAC — راهنمای فارسی](KootletLandAC/README.fa.md)
- [KootletClient documentation](KootletClient/README.md)
