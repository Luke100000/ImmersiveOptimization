package net.conczin.immersive_optimization;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.TicketStorage;

public class NeoForgeForcedChunkLookup implements CommonClass.ForcedChunkLookup {
    @Override
    public boolean isForced(ServerLevel level, long chunk) {
        if (level.getForceLoadedChunks().contains(chunk)) {
            return true;
        }

        TicketStorage data = level.getDataStorage().get(TicketStorage.TYPE);
        if (data == null) {
            return false;
        }

        for (Ticket ticket : data.getTickets(chunk)) {
            if (ticket.getTicketLevel() == ChunkMap.FORCED_TICKET_LEVEL && ticket.getType().persist() && ticket.getType().doesLoad()) {
                return true;
            }
        }
        return false;
    }
}
