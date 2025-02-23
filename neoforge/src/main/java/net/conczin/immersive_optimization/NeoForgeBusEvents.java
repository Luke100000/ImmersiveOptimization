package net.conczin.immersive_optimization;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class NeoForgeBusEvents {
    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        Commands.register(event.getDispatcher());
    }
}