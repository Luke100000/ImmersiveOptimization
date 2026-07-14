package net.conczin.immersive_optimization;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.conczin.immersive_optimization.mixin.TicketStorageAccessor;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.TicketStorage;
import net.neoforged.neoforge.common.NeoForgeMod;

public class NeoForgeForcedChunkLookup implements CommonClass.ForcedChunkLookup {
    @Override
    public LongSet getForcedChunks(ServerLevel level) {
        LongSet chunks = new LongOpenHashSet(level.getForceLoadedChunks());

        TicketStorage data = level.getDataStorage().get(TicketStorage.TYPE);
        if (data == null) {
            return chunks;
        }

        chunks.addAll(((TicketStorageAccessor) data).immersiveOptimization$getChunksWithTicketThat(ticket -> {
            TicketType type = ticket.getType();
            return ticket.getTicketLevel() == ChunkMap.FORCED_TICKET_LEVEL
                   && (type == NeoForgeMod.BLOCK_TICKET.value()
                       || type == NeoForgeMod.BLOCK_WITH_NATURAL_SPAWNING_TICKET.value()
                       || type == NeoForgeMod.ENTITY_TICKET.value()
                       || type == NeoForgeMod.ENTITY_WITH_NATURAL_SPAWNING_TICKET.value());
        }));
        return chunks;
    }
}
