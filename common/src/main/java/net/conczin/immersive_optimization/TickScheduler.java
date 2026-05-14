package net.conczin.immersive_optimization;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.conczin.immersive_optimization.config.Config;
import net.conczin.immersive_optimization.mixin.EntityTickListAccessor;
import net.conczin.immersive_optimization.mixin.ServerLevelAccessor;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
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
                    // Tick levels
                    for (ServerLevel level : scheduler.knownLevels.values()) {
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
        public int trackingCulledEntities = 0;
        public int viewportCulledEntities = 0;

        public void reset() {
            tickRate = 0;
            entities = 0;
            distanceCulledEntities = 0;
            trackingCulledEntities = 0;
            viewportCulledEntities = 0;
        }
    }

    public static class LevelData {
        public boolean active;

        public long tick = 0;
        public long time = 0;

        public Stats stats = new Stats();
        public Stats previousStats = new Stats();

        public int stressedTicks = 0;
        public int lifeTimeStressedTicks = 0;

        public volatile Int2IntOpenHashMap priorities = new Int2IntOpenHashMap();

        public Map<Long, Integer> blockEntityPriorities = new ConcurrentHashMap<>();

        public LevelData(Identifier identifier) {
            active = Config.getInstance().dimensions.getOrDefault(identifier.toString(), true);
        }

        public int getEntities() {
            return previousStats.entities;
        }

        public String toLog() {
            return "Rate %2.1f%%, %d entities, %d stress, %d stressed | culled %2.1f%% distance, %2.1f%% tracking, %2.1f%% viewport".formatted(
                    previousStats.tickRate / previousStats.entities * 100,
                    previousStats.entities,
                    stressedTicks,
                    lifeTimeStressedTicks,
                    (float) previousStats.distanceCulledEntities / previousStats.entities * 100,
                    (float) previousStats.trackingCulledEntities / previousStats.entities * 100,
                    (float) previousStats.viewportCulledEntities / previousStats.entities * 100
            );
        }
    }

    public final Map<Identifier, LevelData> levelData = new ConcurrentHashMap<>();
    public final Map<Identifier, ServerLevel> knownLevels = new ConcurrentHashMap<>();

    @Nullable
    public LevelData getLevelData(Level level) {
        return levelData.get(level.dimension().identifier());
    }

    public void reset() {
        levelData.clear();
        knownLevels.clear();
        frustum = null;
    }

    public void startLevelTick(ServerLevel level) {
        knownLevels.put(level.dimension().identifier(), level);

        LevelData data = getLevelData(level);
        if (data == null) return;

        // Update level stress status
        int stressedThreshold = Config.getInstance().stressedThreshold;
        boolean stressed = stressedThreshold > 0 && server.getAverageTickTimeNanos() > stressedThreshold * 1_000_000L;
        if (stressed) {
            data.stressedTicks = Math.min(MAX_STRESS_TICKS, data.stressedTicks + 2);
            data.lifeTimeStressedTicks++;
        } else {
            data.stressedTicks = Math.max(0, data.stressedTicks - 1);
        }

        data.tick++;
        data.time = System.nanoTime();
    }

    void tickLevel(ServerLevel level) {
        LevelData data = levelData.computeIfAbsent(level.dimension().identifier(), LevelData::new);
        long tick = level.getGameTime();

        Stats previousStats = data.previousStats;
        data.previousStats = data.stats;
        data.stats = previousStats;
        data.stats.reset();

        // Clear priorities every n seconds to avoid memory leaks
        if (tick % CLEAR_BLOCK_ENTITIES_INTERVAL == 0) {
            data.blockEntityPriorities.clear();
        }

        // Entity culling disabled
        if (!Config.getInstance().enableEntities) {
            data.priorities = new Int2IntOpenHashMap();
            return;
        }

        // Update entity priorities (distributed over n ticks)
        Int2IntOpenHashMap newPriorities = new Int2IntOpenHashMap();
        Int2ObjectMap<Entity> entities = ((EntityTickListAccessor) ((ServerLevelAccessor) level).getEntityTickList()).getActive();
        entities.values().forEach(entity -> {
            if (entity != null) {
                int priority = getPriority(data, level, entity);
                if (priority > 0) {
                    newPriorities.put(entity.getId(), priority);
                }

                data.stats.tickRate += 1.0 / Math.max(1, priority);
                data.stats.entities += 1;
            }
        });
        data.priorities = newPriorities;
    }

    public boolean shouldTick(Entity entity) {
        if (entity.isAlwaysTicking() || INSTANCE == null) return true;

        LevelData data = getLevelData(entity.level());
        if (data == null) return true;

        int priority = data.priorities.getOrDefault(entity.getId(), 0);
        if (priority <= 1) return true;

        return (data.tick + entity.getId()) % priority == 0;
    }

    public int getPriority(LevelData data, ServerLevel level, Entity entity) {
        Config config = Config.getInstance();

        // Blacklist entities
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!config.entities.getOrDefault(id.toString(), true)) return 0;
        if (!config.entities.getOrDefault(id.getNamespace(), true)) return 0;
        if (!config.cullProjectiles && entity instanceof Projectile) return 0;
        if (isForceLoaded(level, entity.chunkPosition().toLong())) return 0;

        // Find the closest player
        double minDistance = 999999.0;
        for (Player player : level.players()) {
            minDistance = Math.min(minDistance, player.distanceToSqr(entity));
        }

        AABB box = entity.getBoundingBox();
        int blocksPerLevel = config.blocksPerLevel;

        boolean integratedAndSinglePlayer = !level.getServer().isDedicatedServer() && level.players().size() == 1;

        // View distance culling
        if (config.enableDistanceCulling && integratedAndSinglePlayer && !entity.shouldRenderAtSqrDistance(minDistance)) {
            blocksPerLevel = config.blocksPerLevelDistanceCulled;
            data.stats.distanceCulledEntities++;
        }

        // Tracking distance culling
        if (config.enableTrackingCulling && blocksPerLevel > config.blocksPerLevelTrackingCulled) {
            int trackingRange = entity.getType().clientTrackingRange();
            int scaledTrackingDistance = level.getServer().getScaledTrackingDistance(trackingRange * 16);
            if (minDistance > scaledTrackingDistance * scaledTrackingDistance) {
                blocksPerLevel = config.blocksPerLevelTrackingCulled;
                data.stats.trackingCulledEntities++;
            }
        }

        // Frustum culling (Only available for integrated servers)
        if (config.enableViewportCulling
            && blocksPerLevel > config.blocksPerLevelViewportCulled
            && integratedAndSinglePlayer
            && frustum != null
            && !frustum.isVisible(box)) {
            blocksPerLevel = config.blocksPerLevelViewportCulled;
            data.stats.viewportCulledEntities++;
        }

        // Assign an optimization level
        double antiStress = 1.0 - (double) data.stressedTicks / MAX_STRESS_TICKS * config.minDecreaseFactor;
        int finalBlocksPerLevel = (int) (blocksPerLevel * antiStress);
        int distanceLevel = (int) ((Math.sqrt(minDistance) - config.minDistance) / Math.max(2, finalBlocksPerLevel));

        // And clamp it to sane numbers
        return Math.min(config.maxLevel, Math.max(1, distanceLevel + 1));
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

    private boolean isForceLoaded(Level level, long chunk) {
        return !Config.getInstance().optimizeForceLoadedChunks && level instanceof ServerLevel serverLevel && CommonClass.isForceLoaded(serverLevel, chunk);
    }

    private int getBlockEntityPriority(Level level, long p) {
        if (isForceLoaded(level, p)) {
            return 0;
        }

        int x = ChunkPos.getX(p);
        int z = ChunkPos.getZ(p);

        if (x < -300000 || x > 300000 || z < -300000 || z > 300000) {
            return -1;
        }

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
