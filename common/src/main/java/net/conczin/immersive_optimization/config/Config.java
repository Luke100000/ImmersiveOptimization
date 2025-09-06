package net.conczin.immersive_optimization.config;

import net.conczin.immersive_optimization.Constants;

import java.util.HashMap;
import java.util.Map;

public final class Config extends JsonConfig {
    private static Config INSTANCE = loadOrCreate(new Config(), Config.class);

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

    @SuppressWarnings("unused")
    public String _documentation = "https://github.com/Luke100000/ImmersiveOptimization/wiki";

    // Enable the mod. If you plan to not use it altogether, uninstall it.
    public boolean enableEntities = true;
    public boolean enableBlockEntities = true;

    // Distance culling reduces the tick rate of entities when not visible due to render distance.
    // (Singleplayer only)
    public boolean enableDistanceCulling = true;
    // Tracking culling reduces the tick rate of entities when not tracked due to distance.
    public boolean enableTrackingCulling = true;
    // Viewport culling slows down entities when outside the camera perspective.
    // (Singleplayer only)
    public boolean enableViewportCulling = true;

    // Every blocksPerLevel, the tick rate will be reduced by 1, offset by initial minDistance to avoid visible glitches.
    // Smaller values increase server performance.
    public int minDistance = 6;
    public int blocksPerLevel = 64;
    public int blocksPerLevelDistanceCulled = 10;
    public int blocksPerLevelTrackingCulled = 10;
    public int blocksPerLevelViewportCulled = 20;
    public int maxLevel = 20;

    // The same for block entities, but without further culling.
    public int blocksPerLevelBlockEntities = 32;

    // The ms of the total server tick time before the server is considered stressed.
    // When stressed, the server will gradually increase the blockedPerLevel by at least minDecreaseFactor.
    // This may increase visual glitches with clients and is a last resort to avoid lag.
    // 0 to turn off.
    public int stressedThreshold = 45;
    public float minDecreaseFactor = 0.25f;

    // Set to "false" to disable scheduling on given dimensions.
    public Map<String, Boolean> dimensions;

    {
        dimensions = new HashMap<>();
        dimensions.put("minecraft:overworld", true);
        dimensions.put("minecraft:the_nether", true);
        dimensions.put("minecraft:the_end", true);
    }

    // Or entities. The blacklist accepts resource locations or namespaces (to blacklist an entire mod).
    public Map<String, Boolean> entities;

    {
        entities = new HashMap<>();

        entities.put("minecraft:player", false);
        entities.put("minecraft:ender_dragon", false);
        entities.put("minecraft:arrow", false);
        entities.put("minecraft:ender_pearl", false);

        entities.put("fromanotherworld:starship", false);

        entities.put("create", false);
        entities.put("valkyrienskies", false);
    }

    public void reload() {
        INSTANCE = loadOrCreate(new Config(), Config.class);
    }
}
