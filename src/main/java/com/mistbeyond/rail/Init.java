package com.mistbeyond.rail;

import com.mistbeyond.registry.CommonRegistrar;
import com.mistbeyond.rail.block.Blocks;
import com.mistbeyond.rail.block.rail.RailSignalBlocks;
import com.mistbeyond.rail.block.rail.RailSignalSide;
import com.mistbeyond.rail.block.rail.RailTrackCellBlock;
import com.mistbeyond.rail.block.rail.RailTrackCellBlockEntities;
import com.mistbeyond.rail.core.rail.RailSignalsBroadcaster;
import com.mistbeyond.rail.core.rail.RailNetworkManager;
import com.mistbeyond.rail.core.rail.RailSignalsPayload;
import com.mistbeyond.rail.entity.rail.Entities;
import com.mistbeyond.rail.item.CreativeTabs;
import com.mistbeyond.rail.item.Items;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Init {
    private static final RailSignalsBroadcaster SIGNALS_BROADCASTER = new RailSignalsBroadcaster();

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Ids.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Ids.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Ids.MOD_ID);

    /**
     * Ghost-preview side of the block signal item stacks (ADR 0012): which side of the facing direction the next
     * placement targets. Toggled with shift + use; persisted and network-synchronized so the client ghost matches
     * the server placement.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RailSignalSide>> RAIL_SIGNAL_SIDE =
            DATA_COMPONENTS.register("rail_signal_side", () -> DataComponentType.<RailSignalSide>builder()
                    .persistent(RailSignalSide.CODEC)
                    .networkSynchronized(RailSignalSide.STREAM_CODEC)
                    .build());

    public static final CommonRegistrar REGISTRAR =
            CommonRegistrar.of(Ids.MOD_ID, Blocks.BLOCKS, Items.ITEMS, BLOCK_ENTITIES, MENUS);

    private Init() {
    }

    public static void registerCommon(IEventBus modBus, ModContainer modContainer) {
        REGISTRAR.registerCommon(modBus, modContainer);
        DATA_COMPONENTS.register(modBus);
        RailTrackCellBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        CreativeTabs.CREATIVE_TABS.register(modBus);
        Entities.ENTITY_TYPES.register(modBus);
        modBus.addListener((RegisterPayloadHandlersEvent event) ->
                event.registrar("1").playToClient(RailSignalsPayload.TYPE, RailSignalsPayload.STREAM_CODEC));
        NeoForge.EVENT_BUS.addListener((LevelEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                RailNetworkManager.of(serverLevel).setSource(RailTrackCellBlock.source(serverLevel));
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            for (ServerLevel serverLevel : event.getServer().getAllLevels()) {
                RailNetworkManager.of(serverLevel).tickAutomation();
            }
            SIGNALS_BROADCASTER.onServerTick(event.getServer());
            RailSignalBlocks.tick(event.getServer());
        });
    }
}
