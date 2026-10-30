# Kootlet Client

PvP client mod for KootletLand (Minecraft 1.8.9, Forge).

**HUD:** Watermark, FPS, Ping, Coords, Direction, Speed, Memory, Clock
**PvP:** CPS, Keystrokes, Combo, Reach, Target HUD, Armor Status, Potion Status
**Visual:** Custom Crosshair, Fullbright, Zoom (smooth + scroll), No Fire, Block Outline
**Utility:** Toggle Sprint, Chat Timestamps
**Extras:** animated KootletLand main menu, in-game menu with per-module settings, 7 accent themes, toast notifications.

**Keys:** `Right Shift` = menu (toggle modules, edit settings, drag HUD boxes), `C` = zoom (rebindable in Controls).

Settings: `config/kootletclient.properties`. Put the server address in `serverip=` (or `DEFAULT_SERVER_IP` in `Config.java`) to get a "Join KootletLand" button on the main menu. `custommenu=false` brings back the vanilla main menu.

## Build
GitHub Actions builds the jar (Artifacts > KootletClient). Put it in `.minecraft/mods` with Forge 1.8.9 (11.15.1.2318).
