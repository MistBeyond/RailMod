package com.mistbeyond.rail.item;

import com.mistbeyond.rail.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ids.MOD_ID);

    static {
        CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.literal("Mistbeyond Rail"))
                .icon(() -> Items.ITEMS.getEntries().stream()
                        .findFirst()
                        .map(holder -> new ItemStack(holder.value()))
                        .orElse(ItemStack.EMPTY))
                .displayItems(Items.ITEMS.getEntries())
                .build());
    }

    private CreativeTabs() {
    }
}
