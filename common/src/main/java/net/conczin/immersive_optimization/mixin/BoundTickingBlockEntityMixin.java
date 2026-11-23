package net.conczin.immersive_optimization.mixin;

import net.conczin.immersive_optimization.TickScheduler;
import net.conczin.immersive_optimization.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoundTickingBlockEntityMixin {
    @Shadow
    @Final
    private BlockEntity blockEntity;

    // Resolved lazily once per ticker
    @Unique
    private int immersiveOptimization$ruleVersion = -1;
    @Unique
    private boolean immersiveOptimization$exempt;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void immersiveOptimization$tick(CallbackInfo ci) {
        Config config = Config.getInstance();
        if (!config.enableBlockEntities) return;

        Level level = blockEntity.getLevel();
        if (level == null) return;

        int ruleVersion = Config.BLOCK_ENTITY_RULE_VERSION.get();
        if (immersiveOptimization$ruleVersion != ruleVersion) {
            immersiveOptimization$exempt = config.isBlacklisted(blockEntity);
            immersiveOptimization$ruleVersion = ruleVersion;
        }
        if (immersiveOptimization$exempt) return;

        BlockPos pos = blockEntity.getBlockPos();
        if (!TickScheduler.INSTANCE.shouldTick(level, ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4))) {
            ci.cancel();
        }
    }
}
