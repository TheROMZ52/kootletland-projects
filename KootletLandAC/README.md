# KootletLandAC

KootletLandAC is an independent, modular Paper anti-cheat for KootletLand.

## Policy

Automatic punishment is intentionally disabled. KootletLandAC detects, correlates, stores evidence, and warns staff. It never bans, kicks, mutes, or otherwise punishes a player automatically.

## Architecture

Player events → normalized player state → physics/context → detection → evidence → confidence → violation buffer → correlation/warning.

The core is independent from GrimAC. Future packet adapters and Grim integration can consume public APIs without coupling the core to Grim internals.

## Checks

Movement: Speed, Fly, NoFall, Jesus, Step, HighJump, LongJump, Timer, Phase, NoWeb, InvalidMovement, Strafe, Motion.

Combat: Reach, KillAura, Aim, AutoClicker, Velocity.

The first implementation uses Paper events and a version-neutral internal model. Packet-level adapters can be added without changing the detection core.

## Commands

`/kac debug <player>`
`/kac verbose <player>`
`/kac alerts`
`/kac stats`
`/kac reload`

## Permissions

`kootletlandac.admin`
`kootletlandac.alerts`
`kootletlandac.bypass`

## Build

Java 21 and Gradle 8.14.3.

`gradle test build`

## Safety

Checks are evidence-based and contextual. Teleports, velocity, liquids, vehicles, environment, and game mode are considered before movement evidence is emitted. Invalid numeric state is treated as a security boundary.

## Status

Version 1.0.0 is a production-oriented foundation with contextual movement/combat signals, statistical timing, dynamic suspicion buffering, cross-engine correlation, explainable evidence, staff-only warnings, configuration reloads, diagnostics, and a public API. Raw packet telemetry and Grim signal ingestion are intentionally reported as unavailable until a real supported adapter is implemented; they are not simulated.

## Changelog

### 1.0.1
- Fewer false positives: elytra, riptide, levitation, Jump Boost, ice/slime/honey (support block, not feet block), slabs/stairs/soul sand (Phase now uses real collision).
- Only real melee hits (`ENTITY_ATTACK`) reach combat checks, so thorns and sweep damage no longer fake Reach or AutoClicker evidence.
- Velocity check now uses the velocity that is actually applied (`PlayerVelocityEvent#getVelocity`).
- Timer rewritten as packet-drift balance (the old interval check could never trigger because moves are processed once per tick).
- Jesus is reachable again (it used to sit behind the water grace).
- Fail-safe: an exception in movement/combat/timer is isolated, logged once, and the group is disabled after 25 errors (`/kac reload` re-enables it).
- Alert messages no longer print `%%`; `InvalidMovement` can be disabled in config.
- Added unit tests (`gradle test`) for TimerBalance, BufferEngine and CorrelationEngine.

## Known limitations
- Only Bukkit events are used (no packet layer yet); `onGround` comes from the client and can be spoofed.
- Behavior engine, lag-compensation engine and Grim/LiteBans/LuckPerms integrations are not implemented.
- Movement/combat statistics are cumulative per session rather than windowed.
- Detection quality has not been validated on a live server with real players yet.
