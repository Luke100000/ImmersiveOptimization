package net.conczin.immersive_optimization;

import com.logisticscraft.occlusionculling.DataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class Provider implements DataProvider {
    private final Level level;

    public Provider(Level level) {
        this.level = level;
    }

    @Override
    public boolean prepareChunk(int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public boolean isOpaqueFullCube(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).isSolidRender(level, pos);
    }
}