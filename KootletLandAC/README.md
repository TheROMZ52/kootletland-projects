# KootletLandAC

Evidence-driven anti-cheat for Paper 1.21.x, built for the KootletLand network.
It does not ban on a single threshold. Every detection produces **evidence with a confidence**, evidence is
buffered, correlated across checks, looked at over time, and only then turned into an alert.
**Automatic punishment is off by default.**

> Status: v1.1.0. Compiles and passes unit tests in CI. It has **not yet been validated on a live server with real
> players**, so treat alerts as leads for staff, not as proof.

## Installation

1. Requirements: Paper 1.21.x, Java 21. GrimAC, LiteBans and LuckPerms are optional.
2. Build with `gradle build` (or download the CI artifact) and put `build/libs/KootletLandAC-*.jar` in `plugins/`.
3. Start the server once, then edit `plugins/KootletLandAC/config.yml`.
4. Give staff `kootletlandac.alerts` (alerts) and `kootletlandac.admin` (commands).

## Configuration

| Key | Meaning |
| --- | --- |
| `settings.alerts` | send alerts to staff with `kootletlandac.alerts` |
| `settings.confidence-warning-threshold` / `violation-warning-threshold` | when an alert (MEDIUM) fires |
| `settings.auto-punishment` | master switch for automatic action, **false by default** |
| `bypass.mode` | `skip` (no checks for bypass players) or `log-only` (checks run, console only) |
| `decision.high-alert-cooldown-ms` | faster alerts at HIGH level |
| `decision.very-high.commands` | console commands for VERY_HIGH, only used if `auto-punishment` is true |
| `checks.movement.*`, `checks.combat.*` | enable or disable each check |
| `integrations.grim`, `integrations.grim-confidence` | optional Grim signal |

`/kac reload` re-reads the config. It does not reset player state, re-register listeners or duplicate tasks, and it re-enables checks that were isolated after errors.

## How a detection becomes an action

```
Paper events -> PaperEventPacketAdapter -> NormalizedPacket -> PacketPipeline -> engine
engine: state model (ground, liquids, effects) + physics + prediction + lag compensation
checks -> Evidence(check, confidence, violation, data)
       -> Buffer / VL -> Correlation (cross-check) -> Behavior (5s/10s/20s/30s windows)
       -> Decision: LOW (monitor) / MEDIUM (alert) / HIGH (alert faster) / VERY_HIGH (configurable action)
```

* **Prediction:** a vanilla gravity model (`vy' = (vy - 0.08) * 0.98`) tracks vertical motion. One bad tick means nothing; Fly needs 6 of the last 8 ticks above the model. Speed needs 3 of the last 6 ticks over the limit.
* **Lag compensation:** ping history, jitter, TPS and MSPT widen tolerances (movement) and the reach allowance (interpolation of a moving victim).
* **Context:** elytra, riptide, levitation, Jump Boost, ice/slime/honey (the block under the player), liquids, vines, cobweb, teleports, knockback and vehicles are handled before any movement flag.
* **Decision levels:** MEDIUM needs confidence and VL above the alert thresholds. HIGH needs 90%+ confidence, VL 10+ and 2+ independent checks. VERY_HIGH needs 95%+, VL 15+, 3+ independent checks and sustained behaviour. A single check can never reach VERY_HIGH.

## Checks

| Check | Signals |
| --- | --- |
| Speed | expected horizontal speed (effects, ice, soul sand, liquids...) vs actual, sustained over ticks, windowed statistics |
| Fly | upward deviation from the gravity model, impossible upward delta |
| NoFall | client says on ground while the server sees no ground below (ground spoof), fall distance |
| Jesus | standing on a liquid surface for 8+ ticks |
| Step, HighJump, LongJump | vertical/horizontal deltas vs the prediction, Jump Boost aware |
| Timer | packet-drift balance (more than 20 movement packets per second), lag-spike safe |
| Phase | real block collision of the player's hitbox |
| NoWeb, Strafe, Motion, InvalidMovement | context specific, plus non-finite and absurd coordinates |
| Velocity | response to the knockback that was actually applied |
| Reach | geometric distance to the hitbox minus a lag-aware allowance, line of sight |
| KillAura | multi-signal: rotation, hit angle, target switching, timing. No single signal is enough |
| Aim | rotation delta shape, sensitivity grid (supporting signal only) |
| AutoClicker | interval distribution: coefficient of variation, entropy, burst timing |

Only real melee hits reach combat checks (`ENTITY_ATTACK`), so thorns and sweep damage cannot fake evidence.

## Commands and permissions

| Command | Description |
| --- | --- |
| `/kac debug <player>` | live movement, prediction, state, lag, velocity, combat, VL, confidence, recent evidence |
| `/kac verbose <player>` | debug plus the raw data of the last evidence |
| `/kac stats` | checks run, detections, alerts, avg/max check time, event time, memory, per-check counts |
| `/kac checks`, `/kac integrations` | enabled checks, integration availability |
| `/kac alerts`, `/kac reload` | alert/punishment state, reload config |

| Permission | Default | Meaning |
| --- | --- | --- |
| `kootletlandac.admin` | op | commands |
| `kootletlandac.alerts` | op | receive alerts |
| `kootletlandac.bypass` | false | exempt from checks (see `bypass.mode`) |

## Grim integration (optional)

If GrimAC is installed, its flags are read through the Grim API event bus and become evidence named `Grim` (default
confidence 0.5). Grim is **one signal, not the truth**: the correlation engine only counts it next to at least two of
our own detections, and Grim alone can never alert or punish. Without Grim the plugin works the same.

The bridge uses reflection (no compile-time dependency) and was written from the public GrimAPI 1.3 documentation.
**It has not been verified against a running Grim server.** If anything does not match, it disables itself and logs
the reason; check `/kac integrations` and the startup diagnostics.

## LiteBans and LuckPerms

No special code is needed. `decision.very-high.commands` run as console commands, so a command such as
`tempban {player} 1d KootletAC: {check} ({confidence}%)` is recorded by LiteBans like any staff action. LuckPerms
resolves `kootletlandac.*` like any other permission node. Both are only detected and reported at startup.

## Debugging and performance

* `/kac debug <player>` is the first stop for a suspicious alert; every alert stores the numbers behind it (expected vs actual position, deviations, ping, jitter, TPS, state).
* The main thread does the movement work once per tick per player that moved; there is no blocking IO and no database in the core. Player data is removed on quit.
* `/kac stats` shows average and maximum check time and event cost. Watch `max` after changes.
* A check that throws is isolated, logged once, and disabled after 25 errors (`/kac stats` lists it). Everything else keeps running.

## Developer API

Published through Bukkit's `ServicesManager` as `ir.kootletland.ac.api.KootletLandACApi`:

```java
KootletLandACApi api = Bukkit.getServicesManager().load(KootletLandACApi.class);
api.getViolationLevel(uuid); api.getConfidence(uuid); api.getBehaviorScore(uuid);
api.listenDetection(evidence -> { /* log, forward... */ });
api.submitEvidence(uuid, evidence);
api.registerCheck(new CustomCheck(){ /* name(), onMove(...), onAttack(...) */ });
api.registerIntegration(integration);
```

`StorageProvider` (default: none) and `DetectionBridge` (default: none, the seam for a future Velocity/central setup)
are interfaces; the core has no database or network dependency.

## Development

```
cd KootletLandAC
gradle test build
```

Unit tests cover the pure logic (timer balance, statistics windows, lag math, reach geometry, behavior, decision,
correlation, rotation analysis, packet validation). Anything that needs a live Paper server is not covered.

## Known limitations

* No raw packet layer yet: only Bukkit events are used, so transaction/keep-alive timing is **NOT IMPLEMENTED** and `onGround` comes from the client. Ground spoofing is detected by comparing it with the server's own collision check.
* Piston pushes and other plugin-driven movement without a velocity event can still look unusual.
* Aim quantization breaks with zoom mods and touch (Bedrock) input, so it only supports other signals.
* Hover with no movement packets at all is not seen, because no movement event fires.
* Grim bridge unverified on a live Grim server. Detection quality is unvalidated on a live server.

## Changelog

### 1.1.0
Spec completion: packet abstraction (normalized packets + pipeline), state model, vertical prediction with multi-tick deviation, lag compensation engine, behavior engine, decision engine with levels, configurable VERY_HIGH action (off by default), bypass modes, optional Grim bridge, custom checks and integrations in the API, windowed statistics (median, percentile, entropy), new combat signals (hit angle, target switching, rotation grid), richer metrics and `/kac debug`.

### 1.0.1
False-positive fixes (elytra, riptide, ice, slabs, thorns/sweep), real Timer, reachable Jesus, correct Velocity vector, fail-safe isolation.
