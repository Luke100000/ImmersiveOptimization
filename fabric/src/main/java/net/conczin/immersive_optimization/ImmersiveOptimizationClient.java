package net.conczin.immersive_optimization;

import net.conczin.immersive_optimization.client.OptimizationDebugEntry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;

public class ImmersiveOptimizationClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DebugScreenEntries.register(OptimizationDebugEntry.ID, new OptimizationDebugEntry());
    }
}
