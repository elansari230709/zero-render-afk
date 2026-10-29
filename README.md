# Zero Render AFK

**Stay connected. Stop rendering. Reduce resource usage.**

Zero Render AFK is a client-side Fabric mod designed for long AFK sessions on multiplayer servers.

It stops rendering and unloads client-side world data while keeping your connection active. It is intended for players who are happy to reconnect when their AFK session ends.

> **Important:** Ending an AFK session disconnects you from the server. Rejoin to resume normal gameplay.

## Features

- **Rendering disabled:** skips world and GUI rendering during an AFK session.
- **Client-side world cleanup:** unloads chunks and removes entities other than your player.
- **Reduced background work:** skips selected client updates and prevents selected incoming world data from rebuilding the unloaded world.
- **In-game settings:** press F8 before starting a session.
- **Clear controls:** green ON and red OFF buttons.
- **Start confirmation:** review the session warning before activating AFK mode.
- **Persistent settings:** your configuration is saved between launches.
- **Optional AFK actions:** configure rotation, experimental jumps, commands, and chat messages independently.

No server-side installation is required.

## Requirements

- Minecraft Java Edition **26.3**
- **Fabric Loader 0.20.1 or newer**
- **Fabric API** for Minecraft 26.3
- **Java 25 or newer**

Use the Minecraft version listed for the release you download. Other versions and mod loaders are not currently supported.

## Installation

1. Install Fabric for Minecraft 26.3.
2. Download Fabric API for the same Minecraft version.
3. Download the Zero Render AFK release JAR.
4. Place both JAR files in your instance’s `mods` folder.
5. Launch Minecraft using that Fabric instance.

Keep only one version of Zero Render AFK in the `mods` folder.

## How to use

1. Join a remote multiplayer server.
2. Wait for the world to finish loading.
3. Stand on solid ground, outside water and without riding a vehicle.
4. Close any open menus.
5. Press **F8** to open Zero Render AFK settings.
6. Configure any optional actions you want to use.
7. Click **Start AFK Session**.
8. Read the confirmation and click **Confirm & Start**.

Rendering stops and client-side world data is discarded. The window may continue to display the last frame; this does not mean the world is still rendering.

To end the session, press **F8** or **Escape**. You will disconnect and can then rejoin the server.

### Menu controls

| Control | Behavior |
| --- | --- |
| F8 before a session | Opens settings |
| Save | Saves settings without starting AFK |
| Cancel / Escape in settings | Closes settings without saving the draft |
| Start AFK Session | Validates settings and opens confirmation |
| Back / Escape in confirmation | Returns to settings |
| Confirm & Start | Saves settings and requests session start |
| F8 / Escape during AFK | Ends the session by disconnecting |

The start conditions are checked again when you confirm.

If the menu does not fit, reduce Minecraft’s GUI Scale or enlarge the game window.

## Optional AFK actions

All actions are disabled by default. Enable the main Anti-AFK switch and each action you want to use.

Each action has its own interval. The first action waits for its configured interval after the session starts; opening the menu or saving settings does not send anything.

### Rotation

Configure:

- Interval
- Horizontal rotation step
- Vertical angle
- Alternation with the starting horizontal orientation

### Packet jumps — experimental

Configure the interval and jump height.

This sends a sequence of movement packets representing a vertical jump. It does **not** run full client-side collision checks or normal movement physics.

Use only on flat, unchanged ground with clear space above. Avoid water, vehicles, low ceilings, and effects that alter jumping.

A server position or rotation correction interrupts the jump and disables further jumps for the rest of that session. Other enabled actions may continue.

### Commands

Send a fixed command at a configured interval.

A leading `/` is optional. Commands use Minecraft’s normal command-sending path.

### Chat

Send a fixed chat message at a configured interval.

Messages must not begin with `/`. Chat uses Minecraft’s normal chat-sending path.

Only use automated actions where the server allows them. These features do not guarantee protection from AFK kicks.

## Performance

In initial testing on the developer’s setup, reported CPU usage dropped from approximately **25–30% to 0–1.5%** during AFK mode.

These are observations from one setup, not guaranteed results. Hardware, server activity, other mods, and background applications can affect resource usage.

### What about RAM?

The mod releases client-side world data, but it does not unload the entire Minecraft application.

Java may retain allocated memory for reuse. Classes, assets, native libraries, and other resources also remain loaded, so the process memory shown by the operating system may stay relatively high.

Lower CPU usage and fewer retained world objects do not necessarily produce an equally large drop in displayed RAM usage.

## Limitations

- **Remote multiplayer only:** low-memory mode does not support singleplayer or a locally hosted integrated server.
- **Reconnect to resume:** the discarded world cannot be restored by simply turning rendering back on.
- **No live view:** the world and menus are not rendered during AFK.
- **No gameplay protection:** your player remains on the server and can still be affected by gameplay.
- **No kick prevention guarantee:** servers may still kick idle players, restart, or close the connection.
- **No sleep support:** system sleep and hibernation suspend Minecraft and interrupt the session.
- **Not a headless client:** Minecraft still runs, with its baseline memory and runtime overhead.

## Compatibility

Compatibility with other mods is not yet broadly verified.

Mods that rely on client-side chunks, entities, rendering, particles, or normal world updates may behave unexpectedly during an AFK session. This can include shaders, minimaps, entity overlays, and other world-dependent features.

For initial testing or long unattended sessions, a dedicated instance with Zero Render AFK and Fabric API is recommended.

Compatibility warnings are precautionary; they are not a confirmed incompatibility list.

## Configuration

Settings are stored in:

`config/zerorenderafk-antiafk.properties`

Use the in-game menu to change settings. If you edit the file manually, close Minecraft first.

## Troubleshooting

### F8 does nothing

Make sure you are in a world with no menu or overlay open. Check Minecraft’s key bindings for conflicts or a remapped Zero Render AFK key.

### The session refuses to start

You must be on a remote server, fully loaded, alive, standing on solid ground, outside water, and not riding a vehicle. Check the chat for the reason.

### The image freezes after starting

This is expected when rendering stops. Press F8 or Escape to end the session and disconnect.

### Memory usage stays high

The mod unloads selected world data, not the entire game. Java and native resources may retain memory. Stable memory usage alone does not indicate a leak.

### Jumps stop working

A server position or rotation correction disables jumps for the remainder of the session. Reconnect before starting a new session.

### I was still kicked for being AFK

Server AFK detection varies. The optional actions do not guarantee that a server will consider you active.

## Reporting issues

Please include:

- Minecraft, Fabric Loader, Fabric API, and mod versions
- Operating system and Java version
- Other installed mods
- Steps to reproduce the problem
- Enabled AFK actions and their intervals
- Relevant logs or a crash report
- Whether it also happens with only Zero Render AFK and Fabric API

For resource-usage reports, include measurements before and after starting AFK, how long the session ran, and the measurement tool used.

Review logs before uploading and remove any private information.
