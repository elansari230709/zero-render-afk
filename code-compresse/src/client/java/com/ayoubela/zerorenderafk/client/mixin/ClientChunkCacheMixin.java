package com.ayoubela.zerorenderafk.client.mixin;

import com.ayoubela.zerorenderafk.client.access.ChunkDataAccess;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin implements ChunkDataAccess {
    // Coordinates only: never retain LevelChunk references in this tracker.
    @Unique private final Set<ChunkPos> zerorenderafk$loaded = new HashSet<>();
    @Unique private int zerorenderafk$centerX;
    @Unique private int zerorenderafk$centerZ;

    @Shadow public abstract void drop(ChunkPos pos);
    @Shadow public abstract void updateViewCenter(int x, int z);
    @Shadow public abstract LevelLightEngine getLightEngine();
    @Shadow public abstract LevelChunk getChunk(int x, int z, ChunkStatus status, boolean load);

    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void zerorenderafk$track(int x, int z, ClientboundLevelChunkPacketData data,
                                   CallbackInfoReturnable<LevelChunk> cir) {
        if (cir.getReturnValue() != null) {
            zerorenderafk$loaded.add(new ChunkPos(x, z));
        }
    }

    @Inject(method = "drop", at = @At("RETURN"))
    private void zerorenderafk$forget(ChunkPos pos, CallbackInfo ci) {
        zerorenderafk$loaded.remove(pos);
    }

    @Inject(method = "updateViewCenter", at = @At("RETURN"))
    private void zerorenderafk$rememberCenter(int x, int z, CallbackInfo ci) {
        zerorenderafk$centerX = x;
        zerorenderafk$centerZ = z;
    }

    @Override
    public int zerorenderafk$unloadTrackedChunks() {
        int count = 0;
        LevelLightEngine lighting = getLightEngine();
        int originalX = zerorenderafk$centerX;
        int originalZ = zerorenderafk$centerZ;
        try {
        for (ChunkPos pos : new ArrayList<>(zerorenderafk$loaded)) {
            // A stored slot can be outside the current view after a center
            // update. Temporarily admit it so the normal validated drop path
            // can unload it. No allocation or server packet is involved.
            updateViewCenter(pos.x(), pos.z());
            LevelChunk chunk = getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false);
            if (chunk == null) continue;

            drop(pos);
            lighting.setLightEnabled(pos, false);
            for (int y = lighting.getMinLightSection(); y < lighting.getMaxLightSection(); y++) {
                SectionPos section = SectionPos.of(pos, y);
                lighting.queueSectionData(LightLayer.BLOCK, section, null);
                lighting.queueSectionData(LightLayer.SKY, section, null);
            }
            for (int i = 0; i < chunk.getSections().length; i++) {
                lighting.updateSectionStatus(SectionPos.of(pos, chunk.getSectionYFromSectionIndex(i)), true);
            }
            count++;
        }
        } finally {
            updateViewCenter(originalX, originalZ);
        }
        zerorenderafk$loaded.clear();
        lighting.runLightUpdates();
        return count;
    }
}
