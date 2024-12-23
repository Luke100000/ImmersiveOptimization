package net.conczin.immersive_optimization.mixin;

import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;


@Mixin(ServerLevel.class)
abstract public class ServerLevelMixin {
    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void immersiveOptimization$tick(BooleanSupplier $$0, CallbackInfo ci) {
        TickScheduler.INSTANCE.startLevelTick((ServerLevel)(Object)this);
    }

    @Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void immersiveOptimization$tickNonPassenger(Entity entity, CallbackInfo ci) {
        if (TickScheduler.INSTANCE != null && !TickScheduler.INSTANCE.shouldTick(entity)) {
            ci.cancel();
        }
    }
}
