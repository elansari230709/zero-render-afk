package com.ayoubela.zerorenderafk.client.mixin;

import com.ayoubela.zerorenderafk.client.antiafk.AntiAfkController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class AntiAfkCorrectionMixin {
    // TAIL runs after main-thread handling and teleport acknowledgements.
    @Inject(method = {"handleMovePlayer", "handleRotatePlayer"}, at = @At("TAIL"))
    private void zerorenderafk$acceptCorrection(CallbackInfo ci) {
        AntiAfkController.corrected(Minecraft.getInstance().player);
    }
}
