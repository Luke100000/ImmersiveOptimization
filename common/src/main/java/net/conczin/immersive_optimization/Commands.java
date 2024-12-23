package net.conczin.immersive_optimization;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.conczin.immersive_optimization.config.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class Commands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("io")
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("report")
                        .executes(context -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append("§l§a[Immersive Optimization Report]§r\n");
                            TickScheduler.INSTANCE.levelData.forEach((key, data) ->
                                    sb.append("%s: %s\n".formatted(key.getPath(), data.toLog())));
                            send(context, sb.toString());
                            return 0;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("config")
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("preset")
                                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("performance")
                                        .executes(context -> {
                                            setConfigPreset(0.5f);
                                            return 0;
                                        })
                                )
                                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("default")
                                        .executes(context -> {
                                            setConfigPreset(1.0f);
                                            return 0;
                                        })
                                )
                                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("quality")
                                        .executes(context -> {
                                            setConfigPreset(2.0f);
                                            return 0;
                                        })
                                )
                        )
                        .then(toggle("enableEntities", enabled -> Config.getInstance().enableEntities = enabled))
                        .then(toggle("enableBlockEntities", enabled -> Config.getInstance().enableBlockEntities = enabled))
                        .then(toggle("enableDistanceCulling", enabled -> Config.getInstance().enableDistanceCulling = enabled))
                        .then(toggle("enableOcclusionCulling", enabled -> Config.getInstance().enableOcclusionCulling = enabled))
                        .then(toggle("enableViewportCulling", enabled -> Config.getInstance().enableViewportCulling = enabled))
                        .then(toggle("enableBudget", enabled -> Config.getInstance().entityTickBudget = enabled ? (new Config()).entityTickBudget : 0))
                        .then(toggle("enabledStress", enabled -> Config.getInstance().stressedThreshold = enabled ? (new Config()).stressedThreshold : 0))
                )
        );
    }

    private static void setConfigPreset(float quality) {
        Config d = new Config();
        Config c = Config.getInstance();
        c.minDistance = (int) (d.minDistance * quality);
        c.blocksPerLevel = (int) (d.blocksPerLevel * quality);
        c.blocksPerLevelDistanceCulled = (int) (d.blocksPerLevelDistanceCulled * quality);
        c.blocksPerLevelViewportCulled = (int) (d.blocksPerLevelViewportCulled * quality);
        c.blocksPerLevelOcclusionCulled = (int) (d.blocksPerLevelOcclusionCulled * quality);
        c.maxLevel = (int) (d.maxLevel / quality);
        c.save();
    }

    private static ArgumentBuilder<CommandSourceStack, ?> toggle(String name, Consumer<Boolean> consumer) {
        return LiteralArgumentBuilder.<CommandSourceStack>literal(name)
                .then(RequiredArgumentBuilder.<CommandSourceStack, Boolean>argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            consumer.accept(BoolArgumentType.getBool(context, "enabled"));
                            TickScheduler.INSTANCE.reset();
                            Config.getInstance().save();
                            send(context, "Config updated!");
                            return 0;
                        })
                );
    }

    private static void send(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), true);
    }
}
