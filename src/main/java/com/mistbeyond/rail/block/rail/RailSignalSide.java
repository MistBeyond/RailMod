package com.mistbeyond.rail.block.rail;

import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import io.netty.buffer.ByteBuf;

/**
 * Side of a rail signal relative to its facing direction: a signal stands on the left or the right side of the
 * track it guards, in the World Grid cell adjacent to the track cell (ADR 0012). "Left" and "right" are defined
 * looking along the signal's facing direction.
 */
public enum RailSignalSide implements StringRepresentable {
    LEFT,
    RIGHT;

    /**
     * String codec, mirroring {@code GridDirection.CODEC}.
     */
    public static final Codec<RailSignalSide> CODEC = Codec.STRING.xmap(RailSignalSide::valueOf, RailSignalSide::name);

    /**
     * Network codec for the ghost-side {@code DataComponentType}: one boolean, right iff true. Declared over the
     * raw {@code ByteBuf} so it satisfies the {@code ? super RegistryFriendlyByteBuf} bound of the component
     * registry.
     */
    public static final StreamCodec<ByteBuf, RailSignalSide> STREAM_CODEC = ByteBufCodecs.BOOL.map(
            right -> right ? RIGHT : LEFT,
            side -> side == RIGHT
    );

    /**
     * The side mirrored across the facing direction.
     */
    public RailSignalSide opposite() {
        return this == LEFT ? RIGHT : LEFT;
    }

    /**
     * X offset of the signal block cell relative to the guarded track cell: the side's perpendicular offset.
     * Looking along {@code facing}, LEFT is towards {@code (dz, -dx)} and RIGHT towards {@code (-dz, dx)}.
     */
    public int offsetX(GridDirection facing) {
        return this == LEFT ? facing.dz() : -facing.dz();
    }

    /**
     * Z offset of the signal block cell relative to the guarded track cell; see {@link #offsetX(GridDirection)}.
     */
    public int offsetZ(GridDirection facing) {
        return this == LEFT ? -facing.dx() : facing.dx();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
