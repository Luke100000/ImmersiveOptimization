package net.conczin.immersive_optimization;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class NeoForgeBusEvents {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        Commands.register(event.getDispatcher());
    }
}