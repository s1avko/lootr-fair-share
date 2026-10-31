package com.fairshare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class ActivePlayers extends SavedData {
    private record Seen(String name, long time) {
        static final Codec<Seen> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Seen::name),
                Codec.LONG.fieldOf("time").forGetter(Seen::time)).apply(i, Seen::new));
    }

    private static final Codec<Map<UUID, Seen>> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Seen.CODEC);
    private static final Factory<ActivePlayers> FACTORY = new Factory<>(() -> new ActivePlayers(Map.of()),
            (tag, registries) -> new ActivePlayers(CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow()), null);

    private final Map<UUID, Seen> seen;

    private ActivePlayers(Map<UUID, Seen> seen) {
        this.seen = new HashMap<>(seen);
    }

    private static ActivePlayers get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "fairshare_players");
    }

    public static void seen(MinecraftServer server, UUID id, String name, long time) {
        ActivePlayers data = get(server);
        data.seen.put(id, new Seen(name, time));
        data.setDirty();
    }

    public static List<String> active(MinecraftServer server) {
        long since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(FairShare.ACTIVE_DAYS.get());
        Map<UUID, String> active = new LinkedHashMap<>();
        server.getPlayerList().getPlayers().forEach(p -> active.put(p.getUUID(), p.getGameProfile().getName()));
        get(server).seen.forEach((id, s) -> {
            if (s.time >= since)
                active.putIfAbsent(id, s.name);
        });
        return new ArrayList<>(active.values());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, seen).getOrThrow();
    }
}
