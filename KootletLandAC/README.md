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

Version 1.0.0 is the initial production-oriented foundation. Packet adapters, richer prediction models, Grim signal ingestion, and expanded statistical models are intentionally isolated as extension points rather than faked as implemented.
