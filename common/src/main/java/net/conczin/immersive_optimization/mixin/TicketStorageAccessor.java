package net.conczin.immersive_optimization.mixin;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Predicate;

@Mixin(TicketStorage.class)
public interface TicketStorageAccessor {
    @Invoker("getAllChunksWithTicketThat")
    LongSet immersiveOptimization$getChunksWithTicketThat(Predicate<Ticket> predicate);
}
