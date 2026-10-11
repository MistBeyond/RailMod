package com.mistbeyond.rail.block.rail;

import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.graph.SignalPlacement;
import com.mistbeyond.rail.api.rail.graph.TrackCellData;
import com.mistbeyond.rail.api.rail.graph.TrackCellDataRecord;
import com.mistbeyond.rail.api.rail.graph.TrackPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RailTrackCellBlockEntity extends BlockEntity implements TrackCellData {
    /**
     * Model-data key of the per-block-entity track geometry for the baked-model rendering route (ADR 0006). The
     * client block model reads this property and generates quads from the placement set; the value is an immutable
     * {@link TrackCellDataRecord} snapshot because model data is read from meshing worker threads.
     */
    public static final ModelProperty<TrackCellData> TRACK_CELL_MODEL_DATA = new ModelProperty<>();

    private Set<TrackPlacement> placements = Set.of();
    private Set<SignalPlacement> signals = Set.of();

    public RailTrackCellBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(RailTrackCellBlockEntities.TRACK_CELL.get(), worldPosition, blockState);
    }

    /**
     * Backwards-compatible overload for single-signal callers.
     */
    public void setData(Set<TrackPlacement> placements, Optional<SignalPlacement> signal) {
        setData(placements, signal.map(Set::of).orElse(Set.of()));
    }

    public void setData(Set<TrackPlacement> placements, Set<SignalPlacement> signals) {
        this.placements = Set.copyOf(placements);
        this.signals = Set.copyOf(signals);
        setChanged();
        requestModelDataUpdate();
    }

    @Override
    public ModelData getModelData() {
        return ModelData.of(TRACK_CELL_MODEL_DATA, new TrackCellDataRecord(cell(), placements, signals));
    }

    /**
     * Runtime-contract: complex block entities MUST use standard NBT persistence and update tags. The update packet
     * carries the placements and signals to the client, so the client-side track graph (F3 overlay, ghost previews)
     * sees the same signal placements as the server.
     */
    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public GridPos cell() {
        BlockPos pos = getBlockPos();
        return new GridPos(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Set<TrackPlacement> placements() {
        return placements;
    }

    @Override
    public Set<SignalPlacement> signals() {
        return signals;
    }

    @Override
    public Optional<SignalPlacement> signal() {
        return signals.stream().findFirst();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.placements = Set.copyOf(input.read("Placements", TrackPlacement.CODEC.listOf()).orElse(List.of()));
        // Backwards compatible: old saves used single "Signal", new saves use "Signals" list.
        List<SignalPlacement> migrated = input.read("Signal", SignalPlacement.CODEC).map(List::of).orElse(List.of());
        List<SignalPlacement> list = input.read("Signals", SignalPlacement.CODEC.listOf()).orElse(migrated);
        this.signals = Set.copyOf(list);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Placements", TrackPlacement.CODEC.listOf(), placements.stream().toList());
        if (!signals.isEmpty()) {
            output.store("Signals", SignalPlacement.CODEC.listOf(), signals.stream().toList());
            // Keep single "Signal" for backwards compatibility when exactly one signal exists (optional, not required for load).
            if (signals.size() == 1) {
                output.store("Signal", SignalPlacement.CODEC, signals.iterator().next());
            }
        }
    }
}
