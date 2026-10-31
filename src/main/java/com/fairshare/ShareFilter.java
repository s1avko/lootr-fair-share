package com.fairshare;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import noobanidus.mods.lootr.common.api.data.LootFiller;
import noobanidus.mods.lootr.common.api.filter.ILootrFilter;
import noobanidus.mods.lootr.common.api.filter.ILootrFilterProvider;

public final class ShareFilter implements ILootrFilterProvider, ILootrFilter {
    @Override
    public List<ILootrFilter> getFilters() {
        return List.of(this);
    }

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    public String getName() {
        return FairShare.MODID;
    }

    @Override
    public boolean mutate(ObjectArrayList<ItemStack> loot, LootFiller.LootFillerState state, LootContext context, RandomSource random) {
        MinecraftServer server = state == null || state.player() == null ? null : state.player().getServer();
        double share = server == null ? 1 : FairShare.share(ActivePlayers.active(server).size());
        if (share < 1) {
            loot.forEach(stack -> stack.setCount(Mth.floor(stack.getCount() * share + random.nextDouble())));
            loot.removeIf(ItemStack::isEmpty);
        }
        return false;
    }
}
