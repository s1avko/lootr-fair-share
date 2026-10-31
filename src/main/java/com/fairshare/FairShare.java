package com.fairshare;

import java.util.List;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(FairShare.MODID)
public final class FairShare {
    public static final String MODID = "fairshare";

    private static final ModConfigSpec.Builder CONFIG = new ModConfigSpec.Builder();
    static final ModConfigSpec.ConfigValue<List<? extends Number>> SHARES = CONFIG
            .comment("Share of a loot container's contents each player gets, by the number of active players (1, 2, 3, ...).",
                    "More active players than entries use the last entry. The default is 1/sqrt(players): together a",
                    "group of n gets sqrt(n) times what one player alone gets.")
            .defineList("sharePerPlayer", List.of(1.0, 0.71, 0.58, 0.5, 0.45), () -> 0.45,
                    o -> o instanceof Number n && n.doubleValue() >= 0 && n.doubleValue() <= 1);
    static final ModConfigSpec.IntValue ACTIVE_DAYS = CONFIG
            .comment("A player counts as active while online and for this many days after they last played.")
            .defineInRange("activeDays", 7, 1, 365);

    public FairShare(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CONFIG.build());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> seen(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> seen(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> event.getDispatcher().register(Commands.literal("fairshare")
                .executes(context -> {
                    List<String> active = ActivePlayers.active(context.getSource().getServer());
                    int percent = (int) Math.round(share(active.size()) * 100);
                    Component names = active.isEmpty() ? Component.translatableWithFallback("commands.fairshare.none", "none")
                            : Component.literal(String.join(", ", active));
                    context.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.fairshare.active",
                            "%s active player(s) (played in the last %s days): %s. Each gets %s%% of a loot container's contents.",
                            active.size(), ACTIVE_DAYS.get(), names, percent), false);
                    return active.size();
                })));
    }

    private static void seen(Player player) {
        if (player instanceof ServerPlayer p)
            ActivePlayers.seen(p.server, p.getUUID(), p.getGameProfile().getName(), System.currentTimeMillis());
    }

    public static double share(int players) {
        List<? extends Number> shares = SHARES.get();
        return shares.isEmpty() ? 1 : shares.get(Math.min(Math.max(players, 1), shares.size()) - 1).doubleValue();
    }
}
