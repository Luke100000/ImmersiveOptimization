package net.conczin.immersive_optimization.mixin.client;

import net.conczin.immersive_optimization.EntityProfiler;
import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;)V", at = @At("HEAD"))
    private void immersiveOptimization$onSetLevel(CallbackInfo ci) {
        TickScheduler.INSTANCE.reset();
        EntityProfiler.CLIENT.reset();
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void immersiveOptimization$tick(CallbackInfo ci) {
        EntityProfiler.CLIENT.tick();
    }
}