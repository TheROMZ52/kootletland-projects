# Kootlet Client

PvP client mod for KootletLand (Minecraft 1.8.9, Forge).

**HUD:** Watermark, FPS, Ping, Coords, Direction, Speed, Memory, Clock, Health, Sprint Status
**PvP:** CPS, Keystrokes, Combo, Reach, Target HUD (with enemy gear), Armor Status, Potion Status, Item Counter
**Visual:** Custom Crosshair (dynamic + hit marker), Fullbright, Zoom (smooth + scroll), No Fire, Block Outline, Time Changer
**Utility:** Toggle Sprint, Chat Timestamps, Compact Chat, Armor Alert
**Extras:** animated KootletLand main menu, in-game menu with per-module settings, 7 accent themes, toast notifications.

**Keys (Controls menu):** `Toggle HUD` and `Toggle Fullbright` are unbound by default.

**Keys:** `Right Shift` = menu (toggle modules, edit settings, drag HUD boxes), `C` = zoom (rebindable in Controls).

Settings: `config/kootletclient.properties`. Put the server address in `serverip=` (or `DEFAULT_SERVER_IP` in `Config.java`) to get a "Join KootletLand" button on the main menu. `custommenu=false` brings back the vanilla main menu.

## Build
GitHub Actions builds the jar (Artifacts > KootletClient). Put it in `.minecraft/mods` with Forge 1.8.9 (11.15.1.2318).
