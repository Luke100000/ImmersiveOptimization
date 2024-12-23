package net.conczin.immersive_optimization.config;

import net.conczin.immersive_optimization.Constants;

import java.util.Map;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config(), Config.class);

    public Config() {
        super(Constants.MOD_ID);
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    @Override
    int getVersion() {
        return 2;
    }

    // Enable the mod. If you plan to not use it altogether, uninstall it.
    public boolean enableEntities = true;
    public boolean enableBlockEntities = true;

    // Distance culling reduces the tick rate of entities when not visible due to distance.
    public boolean enableDistanceCulling = true;
    // Occlusion culling highly improves visual quality by prioritizing visible entities.
    public boolean enableOcclusionCulling = true;
    // Viewport culling slows down entities when outside the camera perspective.
    public boolean enableViewportCulling = true;

    // Every blocksPerLevel, the tick rate will be reduced by 1, offset by initial minDistance to avoid visible glitches.
    // Smaller values increase server performance.
    public int minDistance = 6;
    public int blocksPerLevel = 64;
    public int blocksPerLevelDistanceCulled = 8;
    public int blocksPerLevelViewportCulled = 20 ;
    public int blocksPerLevelOcclusionCulled = 10;
    public int maxLevel = 20;

    // The max distance entities are occlusion culled.
    // This setting has a cubic memory footprint, and values above the viewDistance are useless (as they get distance culled)
    public int occlusionCullingDistance = 128;

    // When the budget is exceeded, the server will skip all remaining entities, and prioritize them next tick.
    // This math may is slightly biased towards the end of the list.
    // 0 to turn off.
    public double entityTickBudget = 30;

    // The ms of total server tick time before the server is considered stressed.
    // When stressed, the server will gradually increase the blockedPerLevel.
    // This may increase visual glitches with clients and is a last resort to avoid lag.
    // 0 to turn off.
    public int stressedThreshold = 45;

    // Set to "false" to disable scheduling on given dimensions.
    public Map<String, Boolean> dimensions = Map.of(
            "minecraft:overworld", true,
            "minecraft:the_nether", true,
            "minecraft:the_end", true
    );

    // Or entities.
    public Map<String, Boolean> entities = Map.of(
            "minecraft:player", false,
            "minecraft:ender_dragon", false,
            "minecraft:arrow", false
    );
}
