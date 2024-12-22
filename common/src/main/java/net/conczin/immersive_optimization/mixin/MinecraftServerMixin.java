package net.conczin.immersive_optimization.mixin;

import net.conczin.immersive_optimization.EntityProfiler;
import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "tickChildren(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void immersiveOptimization$onTick(BooleanSupplier $$0, CallbackInfo ci) {
        EntityProfiler.SERVER.tick();
        TickScheduler.INSTANCE.tick();
    }

    @Inject(method = "loadLevel()V", at = @At("HEAD"))
    private void immersiveOptimization$loadLevel(CallbackInfo ci) {
        EntityProfiler.SERVER.reset();
        TickScheduler.INSTANCE.reset();
    }
}
