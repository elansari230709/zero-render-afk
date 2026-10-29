package com.ayoubela.zerorenderafk.client.mixin;

import com.ayoubela.zerorenderafk.client.ZeroRenderAFKClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    // These callbacks only read a volatile flag, even if invoked by a network
    // thread. No world mutation or buffer release occurs on that thread.
    @Inject(method = {
        "handleLevelChunkWithLight", "handleLightUpdatePacket", "handleChunksBiomes",
        "handleChunkBlocksUpdate", "handleBlockUpdate", "handleBlockEntityData",
        "handleParticleEvent", "handleAddEntity", "handleAddTransientBlockPacket",
        "handleSoundEvent", "handleSoundEntityEvent"
    }, at = @At("HEAD"), cancellable = true)
    private void zerorenderafk$discardWorldData(CallbackInfo ci) {
        if (ZeroRenderAFKClient.isAfkEnabled()) ci.cancel();
    }

    // Do not enter a new dimension/configuration with a deliberately incomplete
    // world. Reconnecting is the supported recovery path for this prototype.
    @Inject(method = {"handleRespawn", "handleConfigurationStart"},
            at = @At("HEAD"), cancellable = true)
    private void zerorenderafk$endSessionForWorldTransition(CallbackInfo ci) {
        if (ZeroRenderAFKClient.isAfkEnabled()) {
            Minecraft.getInstance().execute(ZeroRenderAFKClient::disableAfk);
            ci.cancel();
        }
    }
    // Keep chunk-batch acknowledgements, keep-alives, teleport acknowledgements,
    // health, inventory, chat and the normal connection lifecycle untouched.
}
