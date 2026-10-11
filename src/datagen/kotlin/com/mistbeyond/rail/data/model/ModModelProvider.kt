@file:Suppress("LombokKotlinCompilerPlugin")

package com.mistbeyond.rail.data.model

import com.mistbeyond.rail.Ids
import com.mistbeyond.rail.block.Blocks
import com.mistbeyond.rail.block.rail.RailSignalBlock
import com.mistbeyond.rail.block.rail.RailTrackCellBlock
import com.mistbeyond.rail.client.rail.model.RailSignalBlockModel
import com.mistbeyond.rail.client.rail.model.TrackCellDynamicModel
import com.mistbeyond.rail.item.Items
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.data.models.ModelProvider
import net.minecraft.client.data.models.MultiVariant
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator
import net.minecraft.client.data.models.blockstates.PropertyDispatch
import net.minecraft.client.data.models.model.ItemModelUtils
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.core.Holder
import net.minecraft.data.PackOutput
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder
import java.util.stream.Stream

class ModModelProvider(output: PackOutput) : ModelProvider(output, Ids.MOD_ID) {
    override fun registerModels(blockModels: BlockModelGenerators, itemModels: ItemModelGenerators) {
        val testTrack = Blocks.BLOCKS.entries.stream()
            .filter { it.id.path == "rail_track_cell" }
            .findFirst()
            .orElseThrow()
            .value()
        val trackModel = Identifier.fromNamespaceAndPath(Ids.MOD_ID, "block/track")
        // The track cells render through the dynamic track cell model (ADR 0006): the same block cannot distinguish a
        // simple from a complex cell via its BlockState (both store one of the four axes), so the dynamic model
        // resolves geometry per block entity from model data and falls back to the matching axis geometry. The
        // datagen simple models (block/track, block/track_diagonal) are still generated below and stay usable.
        val trackCellModel = CustomBlockStateModelBuilder.Simple(TrackCellDynamicModel.INSTANCE)
        blockModels.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(testTrack).with(
                PropertyDispatch.initial(RailTrackCellBlock.DIRECTION).generate { _ ->
                    MultiVariant.of(trackCellModel)
                }
            )
        )
        blockModels.registerSimpleItemModel(testTrack, trackModel)
        // The diagonal 45 straight model is generated programmatically from the same parametric
        // geometry; the hand-written Blockbench export it replaces was not symmetric.
        TrackModelGenerator.generateDiagonal(blockModels)

        // The block signal renders through its own dynamic model (ADR 0012): the fixed signal-light template
        // (block/block_signal) is mirrored element by element, and the facing/side/aspect block state properties
        // drive the rotation and the lit lens. Every state maps to the same dynamic model instance.
        val railSignal = Blocks.BLOCKS.entries.stream()
            .filter { it.id.path == "rail_block_signal" }
            .findFirst()
            .orElseThrow()
            .value()
        val signalModel = CustomBlockStateModelBuilder.Simple(RailSignalBlockModel.INSTANCE)
        blockModels.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(railSignal).with(
                PropertyDispatch.initial(RailSignalBlock.FACING).generate { _ ->
                    MultiVariant.of(signalModel)
                }
            )
        )

        val trainSpawner = Items.ITEMS.entries.stream()
            .filter { it.id.path == "test_train_spawner" }
            .findFirst()
            .orElseThrow()
            .value()
        itemModels.generateFlatItem(trainSpawner, net.minecraft.world.item.Items.MINECART, ModelTemplates.FLAT_ITEM)
        // The block signal item uses the fixed signal-light template as its model (inventory, hand, ground, GUI
        // display transforms included, ADR 0012). The item definition simply references the existing
        // block/block_signal model file; no item model JSON is generated for it.
        val itemRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM
        val blockSignalItem = itemRegistry.getValue(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "rail_block_signal"))
        if (blockSignalItem !== net.minecraft.world.item.Items.AIR) {
            itemModels.itemModelOutput.accept(
                blockSignalItem,
                ItemModelUtils.plainModel(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "block/block_signal"))
            )
        }
        // Placeholder item model for the path signal item: the real path signal model does not exist yet, so we
        // reference a vanilla item texture as a stand-in via datagen (no handwritten JSON, per AGENTS.md).
        val pathSignalItem = itemRegistry.getValue(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "rail_path_signal"))
        if (pathSignalItem !== net.minecraft.world.item.Items.AIR) {
            itemModels.generateFlatItem(
                pathSignalItem,
                net.minecraft.world.item.Items.AMETHYST_SHARD,
                ModelTemplates.FLAT_ITEM
            )
        }
    }

    override fun getKnownBlocks(): Stream<out Holder<Block>> = Blocks.BLOCKS.entries.stream()

    override fun getKnownItems(): Stream<out Holder<Item>> = Items.ITEMS.entries.stream()
}
