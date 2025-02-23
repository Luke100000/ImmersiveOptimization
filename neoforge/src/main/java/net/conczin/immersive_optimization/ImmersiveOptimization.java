package net.conczin.immersive_optimization;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Constants.MOD_ID)
public class ImmersiveOptimization {
    public ImmersiveOptimization(IEventBus IEventBus) {
        CommonClass.init();
        NeoForge.EVENT_BUS.register(new NeoForgeBusEvents());
    }
}