package com.ayoubela.zerorenderafk.client.mixin;

import com.ayoubela.zerorenderafk.client.antiafk.AntiAfkController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class AntiAfkPlayerMixin {
    @Inject(method = "sendChanges", at = @At("HEAD"), cancellable = true)
    private void zerorenderafk$scheduledPackets(CallbackInfo ci) {
        if (AntiAfkController.beforeSync((LocalPlayer) (Object) this)) ci.cancel();
    }
}
