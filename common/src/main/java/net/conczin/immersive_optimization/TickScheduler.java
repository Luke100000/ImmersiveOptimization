package net.conczin.immersive_optimization;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.conczin.immersive_optimization.config.Config;
import net.conczin.immersive_optimization.mixin.EntityTickListAccessor;
import net.conczin.immersive_optimization.mixin.ServerLevelAccessor;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TickScheduler {
    public static TickScheduler INSTANCE = new TickScheduler();

    public static final int THREAD_SLEEP = 500;
    public static final int MAX_STRESS_TICKS = 600;
    public static final int CLEAR_BLOCK_ENTITIES_INTERVAL = 207;
    public static final int CLEAR_ENTITIES_INTERVAL = 12007;

    public MinecraftServer server;
    public FrustumProxy frustum;

    public static void setServer(MinecraftServer server) {
        INSTANCE.server = server;
        INSTANCE.reset();

        // Offload the "heavy" lifting to a separate thread
        Thread worker = new Thread(new Worker(INSTANCE, server), "Immersive Optimization Worker");
        worker.setPriority(Thread.MIN_PRIORITY);
        worker.setDaemon(true);
        worker.start();
    }

    public interface FrustumProxy {
        boolean isVisible(AABB aabb);
    }

    public static class Worker implements Runnable {
        TickScheduler scheduler;
        MinecraftServer server;

        public Worker(TickScheduler scheduler, MinecraftServer server) {
            this.scheduler = scheduler;
            this.server = server;
        }

        @Override
        public void run() {
            while (server.isRunning()) {
                try {
                    // Tick scheduler
                    scheduler.tick();

                    // Tick levels
                    for (ServerLevel level : server.getAllLevels()) {
                        scheduler.tickLevel(level);
                    }

                    //noinspection BusyWait
                    Thread.sleep(THREAD_SLEEP);
                } catch (ConcurrentModificationException | ArrayIndexOutOfBoundsException e) {
                    // Ignore
                } catch (Exception e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
            }
            Constants.LOG.info("Shutting down Immersive Optimization worker");
        }
    }

    public static class Stats {
        public double tickRate = 0;
        public int entities = 0;
        public int distanceCulledEntities = 0;
        public int viewportCulledEntities = 0;

        public void reset() {
            tickRate = 0;
            entities = 0;
            distanceCulledEntities = 0;
            viewportCulledEntities = 0;
        }
    }

    public static class LevelData {
        public boolean active;

        public long tick = 0;
        public long time = 0;
        public long budget = 50_000_000;
        public boolean outOfBudget = false;

        public Stats stats = new Stats();
        public Stats previousStats = new Stats();

        public int stressedTicks = 0;
        public int lifeTimeStressedTicks = 0;
        public int lifeTimeBudgetTicks = 0;

        public Map<Integer, Integer> budgeted = new ConcurrentHashMap<>();
        public Map<Integer, Integer> priorities = new ConcurrentHashMap<>();

        public Map<Long, Integer> blockEntityPriorities = new ConcurrentHashMap<>();


        public LevelData(ResourceLocation location) {
            active = Config.getInstance().dimensions.getOrDefault(location.toString(), true);
        }

        public String toLog() {
            return "Rate %2.1f%%, %d stress, %d stressed, %d budgeted, culled %2.1f%% distance, %2.1f%% viewport".formatted(
                    previousStats.tickRate / previousStats.entities * 100,
                    stressedTicks,
                    lifeTimeStressedTicks,
                    lifeTimeBudgetTicks,
                    (float) previousStats.distanceCulledEntities / previousStats.entities * 100,
                    (float) previousStats.viewportCulledEntities / previousStats.entities * 100
            );
        }
    }

    public final Map<ResourceLocation, LevelData> levelData = new ConcurrentHashMap<>();

    @Nullable
    public LevelData getLevelData(Level level) {
        return levelData.get(level.dimension().location());
    }

    public void reset() {
        levelData.clear();
        frustum = null;
    }

    public void startLevelTick(ServerLevel level) {
        LevelData data = getLevelData(level);
        if (data == null) return;

        // Update level stress status
        int stressedThreshold = Config.getInstance().stressedThreshold;
        boolean stressed = stressedThreshold > 0 && server.getAverageTickTime() > stressedThreshold;
        if (data.outOfBudget || stressed) {
            data.stressedTicks = Math.min(MAX_STRESS_TICKS, data.stressedTicks + 2);
            if (stressed) data.lifeTimeStressedTicks++;
            if (data.outOfBudget) data.lifeTimeBudgetTicks++;
            data.outOfBudget = false;
        } else {
            data.stressedTicks = Math.max(0, data.stressedTicks - 1);
        }

        data.tick++;
        data.time = System.nanoTime();
    }

    void tick() {
        // Count total entities
        int totalEntities = 0;
        for (LevelData data : levelData.values()) {
            totalEntities += data.stats.entities;
        }

        // And assign a budget to each level
        for (LevelData data : levelData.values()) {
            double budget = Config.getInstance().entityTickBudget;
            data.budget = budget > 0 ? (long) (budget * data.stats.entities / (totalEntities + 1) * 1_000_000) : 0;
        }
    }

    void tickLevel(Level level) {
        LevelData data = levelData.computeIfAbsent(level.dimension().location(), LevelData::new);
        long tick = level.getGameTime();

        Stats previousStats = data.previousStats;
        data.previousStats = data.stats;
        data.stats = previousStats;
        data.stats.reset();

        // Clear priorities every n seconds to avoid memory leaks
        if (tick % CLEAR_ENTITIES_INTERVAL == 0) {
            data.budgeted.clear();
            data.priorities.clear();
        }
        if (tick % CLEAR_BLOCK_ENTITIES_INTERVAL == 0) {
            data.blockEntityPriorities.clear();
        }

        // Entity culling disabled
        if (!Config.getInstance().enableEntities) {
            data.priorities.clear();
            return;
        }

        // Update entity priorities (distributed over n ticks)
        Int2ObjectMap<Entity> entities = ((EntityTickListAccessor) ((ServerLevelAccessor) level).getEntityTickList()).getActive();
        entities.values().forEach(entity -> {
            if (entity != null) {
                int priority = getPriority(data, level, entity);
                if (priority > 0) {
                    data.priorities.put(entity.getId(), priority);
                } else {
                    data.priorities.remove(entity.getId());
                }

                data.stats.tickRate += 1.0 / Math.max(1, priority);
                data.stats.entities += 1;
            }
        });
    }

    public boolean shouldTick(Entity entity) {
        if (entity.noCulling || INSTANCE == null) {
            return true;
        }

        LevelData data = getLevelData(entity.level());
        if (data == null) {
            return true;
        }

        int priority = data.priorities.getOrDefault(entity.getId(), 0);
        if (priority <= 1) {
            return true;
        }

        // No more resources, apply stress instead
        if (data.outOfBudget) {
            data.budgeted.put(entity.getId(), data.budgeted.getOrDefault(entity.getId(), 0) + 1);
            return false;
        }

        // Check if we are still within budget
        long delta = System.nanoTime() - data.time;
        if (data.budget > 0 && delta > data.budget) {
            data.outOfBudget = true;
        }

        int budgeted = data.budgeted.getOrDefault(entity.getId(), 0);
        if ((data.tick + entity.getId()) % Math.max(1, priority - budgeted) == 0) {
            // Relax again
            if (budgeted > 0) {
                data.budgeted.put(entity.getId(), budgeted - 1);
            }
            return true;
        } else {
            return false;
        }
    }

    public int getPriority(LevelData data, Level level, Entity entity) {
        // Blacklist entities
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (!Config.getInstance().entities.getOrDefault(id, true)) {
            return 0;
        }

        // Find the closest player
        double minDistance = Double.MAX_VALUE;
        for (Player player : level.players()) {
            minDistance = Math.min(minDistance, player.distanceToSqr(entity));
        }

        AABB box = entity.getBoundingBox();
        int blocksPerLevel = Config.getInstance().blocksPerLevel;

        // View distance culling
        if (Config.getInstance().enableDistanceCulling && !entity.shouldRenderAtSqrDistance(minDistance)) {
            blocksPerLevel = Config.getInstance().blocksPerLevelDistanceCulled;
            data.stats.distanceCulledEntities++;
        }

        // Frustum culling (Only available for integrated servers)
        if (Config.getInstance().enableViewportCulling
            && blocksPerLevel > Config.getInstance().blocksPerLevelViewportCulled
            && level.players().size() == 1
            && frustum != null
            && !frustum.isVisible(box)) {
            blocksPerLevel = Config.getInstance().blocksPerLevelViewportCulled;
            data.stats.viewportCulledEntities++;
        }

        // Assign an optimization level
        double antiStress = 1.0 - (double) data.stressedTicks / MAX_STRESS_TICKS;
        int finalBlocksPerLevel = (int) (blocksPerLevel * antiStress);
        int distanceLevel = (int) ((Math.sqrt(minDistance) - Config.getInstance().minDistance) / Math.max(2, finalBlocksPerLevel));

        // And clamp it to sane numbers
        return Math.min(Config.getInstance().maxLevel, Math.max(1, distanceLevel + 1));
    }

    public boolean shouldTick(Level level, long pos) {
        if (!Config.getInstance().enableBlockEntities) {
            return true;
        }
        LevelData data = getLevelData(level);
        if (data == null) {
            return true;
        }
        int priority = data.blockEntityPriorities.computeIfAbsent(pos, p -> this.getBlockEntityPriority(level, p));
        return priority < 1 || (level.getGameTime() + pos) % priority == 0;
    }

    private int getBlockEntityPriority(Level level, long p) {
        int x = ChunkPos.getX(p);
        int z = ChunkPos.getZ(p);
        double minDistance = Double.MAX_VALUE;
        for (Player player : level.players()) {
            double dx = SectionPos.blockToSectionCoord(player.getX()) - x;
            double dz = SectionPos.blockToSectionCoord(player.getZ()) - z;
            double distance = dx * dx + dz * dz;
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return Math.min((int) ((Math.sqrt(minDistance * 16) - Config.getInstance().minDistance) / Config.getInstance().blocksPerLevelBlockEntities) + 1, Config.getInstance().maxLevel);
    }
}
