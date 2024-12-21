package net.conczin.immersive_optimization.mixin;

import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public class LevelRendererFabricMixin {
    @ModifyArgs(method = "renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private void immersiveOptimization$renderLevel(Args args) {
        Entity entity = args.get(0);
        TickScheduler.LevelData data = TickScheduler.INSTANCE.getLevelData(entity.level());
        int priority = data.priorities.getOrDefault(entity.getId(), 0);
        if (priority > 1) {
            // Adjust the tickTime to match for the extended tick duration
            long last = data.lastUpdates.getOrDefault(entity.getId(), 0L);
            float delta = Math.max(0, entity.level().getGameTime() - last - 1);
            float oldTickTime = args.get(4);
            args.set(4, (delta + oldTickTime) / priority);
        }
    }
}
