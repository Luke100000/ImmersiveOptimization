package net.conczin.immersive_optimization;

import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class ImmersiveOptimization {
    public ImmersiveOptimization() {
        CommonClass.forcedChunkLookup = new NeoForgeForcedChunkLookup();
        CommonClass.init();
    }
}
