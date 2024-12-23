package net.conczin.immersive_optimization.mixin;

import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "shouldTickBlocksAt(J)Z", at = @At("HEAD"), cancellable = true)
    public void immersiveOptimization$shouldTickBlocksAt(long pos, CallbackInfoReturnable<Boolean> cir) {
        if (!TickScheduler.INSTANCE.shouldTick((Level) (Object) this, pos)) {
            cir.setReturnValue(false);
        }
    }
}
