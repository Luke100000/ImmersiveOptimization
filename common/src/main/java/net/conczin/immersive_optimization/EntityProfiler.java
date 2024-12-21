package net.conczin.immersive_optimization;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityProfiler {
    public static final int SAMPLES = 200;
    public static EntityProfiler SERVER = new EntityProfiler();
    public static EntityProfiler CLIENT = new EntityProfiler();

    public static class EntityData {
        public String name;
        public int samples;
        public long time = 0;
        public long max = 0;

        public int getSamples() {
            return samples / SAMPLES;
        }

        public long getTime() {
            return time / SAMPLES;
        }

        public double getAverage() {
            return (double) time / samples / SAMPLES;
        }

        public EntityData(String name) {
            this.name = name;
        }
    }

    public int tick = 0;
    public Map<String, EntityData> current = new ConcurrentHashMap<>();
    public Map<String, EntityData> previous = new ConcurrentHashMap<>();

    public void reset() {
        current.clear();
        previous.clear();
    }

    public void tick() {
        tick++;

        if (tick % SAMPLES == 0) {
            previous = current;
            current = new ConcurrentHashMap<>();
        }
    }

    public void logEntity(Entity entity, long delta) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        EntityData data = current.computeIfAbsent(name, EntityData::new);
        data.time += delta;
        data.max = Math.max(data.max, delta);
        data.samples++;
    }

    public Iterable<EntityData> getTopEntities(int n) {
        return previous.values().stream().sorted((a, b) -> Double.compare(b.time, a.time)).limit(n)::iterator;
    }
}
