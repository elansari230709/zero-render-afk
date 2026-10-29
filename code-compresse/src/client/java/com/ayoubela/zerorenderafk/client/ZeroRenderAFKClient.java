package com.ayoubela.zerorenderafk.client;

import com.ayoubela.zerorenderafk.client.access.ChunkDataAccess;
import com.ayoubela.zerorenderafk.client.access.LevelDataAccess;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ayoubela.zerorenderafk.client.access.SearchDataAccess;

public class ZeroRenderAFKClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("zerorenderafk");
    private static volatile boolean afkEnabled;
    private static boolean disconnectRequested;
    private static ClientLevel sessionLevel;

    public static boolean isAfkEnabled() {
        return afkEnabled;
    }

    // Called on the game thread. Never resume a deliberately incomplete world.
    public static void disableAfk() {
        Minecraft client = Minecraft.getInstance();
        if (afkEnabled && client.level != null) {
            if (!disconnectRequested) {
                disconnectRequested = true;
                client.level.disconnect(Component.literal(
                    "AFK session ended. Rejoin the server to resume playing."
                ));
            }
            return;
        }
        afkEnabled = false;
        disconnectRequested = false;
        sessionLevel = null;
    }

    private static void enterAfk(Minecraft client) {
        if (client.level == null || client.player == null || client.getConnection() == null) return;
        if (client.hasSingleplayerServer()) {
            client.player.sendSystemMessage(Component.literal(
                "[Zero Render AFK] Low-memory mode requires a remote server."
            ));
            return;
        }
        if (!client.getConnection().hasClientLoaded() || client.gui.screen() != null
            || client.gui.overlay() != null || client.player.isPassenger()
            || !client.player.onGround() || client.player.isInWater()
            || client.player.isDeadOrDying()) {
            client.player.sendSystemMessage(Component.literal(
                "[Zero Render AFK] Wait until loaded, close menus, and stand on solid ground without a vehicle."
            ));
            return;
        }

        ClientLevel level = client.level;
        int before = level.getChunkSource().getLoadedChunksCount();
        int entitiesBefore = level.getEntityCount();
        KeyMapping.releaseAll();
        client.player.input = new ClientInput();
        client.player.setSprinting(false);
        if (client.gameMode != null) client.gameMode.stopDestroyBlock();
        client.setCameraEntity(client.player);
        sessionLevel = level;
        disconnectRequested = false;
        afkEnabled = true;

        try {
            // Release search callbacks and results retaining old worlds.
            ((SearchDataAccess) client.getConnection().searchTrees())
                .zerorenderafk$releaseSearchData();

            // Clear queued lambdas retaining packet/light data before unloading.
            ((LevelDataAccess) level).zerorenderafk$clearPendingLighting();

            ArrayList<Entity> others = new ArrayList<>();
            for (Entity entity : level.entitiesForRendering()) {
                if (entity != client.player) others.add(entity);
            }
            for (Entity entity : others) {
                level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
            }

            int removed = ((ChunkDataAccess) level.getChunkSource()).zerorenderafk$unloadTrackedChunks();
            level.clearTintCaches();

            // Detach preparation data and reset renderer-owned world data.
            RenderSystem.executePendingTasks();
            client.levelExtractor.setLevel(null);
            client.gameRenderer.gameRenderState().levelRenderState.reset();
            client.levelRenderer.resetLevelRenderData();
            client.particleEngine.setLevel(level);
            client.getSoundManager().stop();

            int remaining = level.getChunkSource().getLoadedChunksCount();
            LOGGER.info("[Zero Render AFK] Chunks: {} -> {}; unloaded: {}; entities: {} -> {}",
                before, remaining, removed, entitiesBefore, level.getEntityCount());
            if (remaining != 0) {
                throw new IllegalStateException("Some chunks were not tracked. Reconnect after installing this patch.");
            }
            LOGGER.info("[Zero Render AFK] Low-memory AFK enabled. F8/Escape ends the connection. No GC is forced.");
        } catch (RuntimeException exception) {
            LOGGER.error("[Zero Render AFK] Cleanup failed; disconnecting instead of resuming an incomplete world", exception);
            disableAfk();
        }
    }

    @Override
    public void onInitializeClient() {
        KeyMapping toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.zerorenderafk.toggle", InputConstants.KEY_F8, KeyMapping.Category.MISC
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                disableAfk();
                while (toggleKey.consumeClick()) { }
                return;
            }
            boolean pressed = false;
            while (toggleKey.consumeClick()) pressed = true;
            if (afkEnabled) {
                if (pressed || client.level != sessionLevel || client.player == null
                    || client.gui.screen() != null || client.gui.overlay() != null
                    || client.player.isDeadOrDying() || client.player.isPassenger()) {
                    disableAfk();
                }
            } else if (pressed) {
                enterAfk(client);
            }
        });
    }
}
