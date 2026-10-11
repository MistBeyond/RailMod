package com.mistbeyond.rail.item.rail;

import com.mistbeyond.registry.RegisterItem;
import com.mistbeyond.registry.SubscribeRegistration;
import com.mistbeyond.registry.impl.ItemRegistration;
import com.mistbeyond.rail.api.rail.dispatch.RailTrainId;
import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.graph.RailGraphView;
import com.mistbeyond.rail.block.rail.RailTrackCellBlock;
import com.mistbeyond.rail.core.rail.RailNetworkManager;
import com.mistbeyond.rail.entity.rail.Entities;
import com.mistbeyond.rail.entity.rail.TestTrainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

@RegisterItem
@SuppressWarnings("unused")
public class TestTrainSpawnerItem extends Item {
    public TestTrainSpawnerItem(Item.Properties properties) {
        super(properties);
    }

    @SubscribeRegistration
    @SuppressWarnings("unused")
    private static void register(ItemRegistration registration) {
        registration.register(
                "test_train_spawner",
                TestTrainSpawnerItem::new,
                properties -> properties.stacksTo(1)
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = context.getClickedPos();
        if (!(context.getLevel().getBlockState(clicked).getBlock() instanceof RailTrackCellBlock)) {
            return InteractionResult.PASS;
        }

        GridPos startPos = new GridPos(clicked.getX(), clicked.getY(), clicked.getZ());
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        RailNetworkManager manager = RailNetworkManager.of(serverLevel);
        manager.setSource(RailTrackCellBlock.source(serverLevel));
        RailTrainId trainId = new RailTrainId("train-" + serverLevel.getGameTime());
        if (manager.spawnTrain(trainId, startPos).isPresent()) {
            return InteractionResult.FAIL;
        }
        RailGraphView graph = manager.graphAt(startPos);

        TestTrainEntity train = Entities.TEST_TRAIN.get().create(
                serverLevel,
                EntitySpawnReason.SPAWN_ITEM_USE
        );
        if (train != null) {
            train.begin(graph, startPos, trainId);
            train.setPos(clicked.getX() + 0.5, clicked.getY() + 0.5, clicked.getZ() + 0.5);
            serverLevel.addFreshEntity(train);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.FAIL;
    }
}
