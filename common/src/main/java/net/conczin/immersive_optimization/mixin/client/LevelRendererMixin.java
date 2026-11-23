package net.conczin.immersive_optimization.mixin.client;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.conczin.immersive_optimization.TickScheduler;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void immersiveOptimization$render(GraphicsResourceAllocator graphicsResourceAllocator, boolean renderBlockOutline, CameraRenderState cameraRenderState, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean shouldRenderSky, boolean consistentDepthRequired, CallbackInfo ci) {
        TickScheduler.INSTANCE.frustum = aabb -> cameraRenderState.cullFrustum != null && cameraRenderState.cullFrustum.isVisible(aabb.inflate(1.0));
    }
}
