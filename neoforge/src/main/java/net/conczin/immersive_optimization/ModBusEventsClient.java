package net.conczin.immersive_optimization;

import net.conczin.immersive_optimization.client.OptimizationDebugEntry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ModBusEventsClient {
    @SubscribeEvent
    public static void onRegisterDebugScreenEntries(RegisterDebugEntriesEvent event) {
        event.register(OptimizationDebugEntry.ID, new OptimizationDebugEntry());
    }
}