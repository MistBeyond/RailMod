package com.mistbeyond.rail.block.rail;

import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mistbeyond.rail.api.rail.graph.GridPos;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailSignalBlockTest {
    @Test
    void signalBlockStandsOnTheChosenSideOfTheFacingDirection() {
        GridPos cell = new GridPos(10, 0, -4);
        // Looking north, left is west and right is east.
        assertEquals(new BlockPos(9, 0, -4), RailSignalBlock.signalPos(cell, GridDirection.NORTH, RailSignalSide.LEFT));
        assertEquals(new BlockPos(11, 0, -4), RailSignalBlock.signalPos(cell, GridDirection.NORTH, RailSignalSide.RIGHT));
        // Looking east, left is north and right is south.
        assertEquals(new BlockPos(10, 0, -5), RailSignalBlock.signalPos(cell, GridDirection.EAST, RailSignalSide.LEFT));
        assertEquals(new BlockPos(10, 0, -3), RailSignalBlock.signalPos(cell, GridDirection.EAST, RailSignalSide.RIGHT));
    }

    @Test
    void backingCellInvertsSignalPlacementForEveryFacingAndSide() {
        GridPos cell = new GridPos(-3, 2, 7);
        for (GridDirection facing : GridDirection.values()) {
            for (RailSignalSide side : RailSignalSide.values()) {
                BlockPos blockPos = RailSignalBlock.signalPos(cell, facing, side);
                assertEquals(cell, RailSignalBlock.backingCell(blockPos, facing, side),
                        facing + "/" + side + " must round-trip to the guarded cell");
                assertTrue(RailSignalBlock.candidatePositions(cell, facing).contains(blockPos),
                        "the placed signal must be one of the two candidates");
            }
        }
        // The two candidates are exactly the left and the right adjacent cell, both on the same Y level.
        List<BlockPos> candidates = RailSignalBlock.candidatePositions(cell, GridDirection.SOUTH_WEST);
        assertEquals(2, candidates.size());
        assertTrue(candidates.stream().allMatch(pos -> pos.getY() == cell.y()));
    }

    @Test
    void sidesAreMutuallyOpposite() {
        assertEquals(RailSignalSide.RIGHT, RailSignalSide.LEFT.opposite());
        assertEquals(RailSignalSide.LEFT, RailSignalSide.RIGHT.opposite());
        // Left of north (west) mirrors right of south (west): opposite facings swap the same world side.
        assertEquals(RailSignalBlock.signalPos(new GridPos(0, 0, 0), GridDirection.NORTH, RailSignalSide.LEFT),
                RailSignalBlock.signalPos(new GridPos(0, 0, 0), GridDirection.SOUTH, RailSignalSide.RIGHT));
    }
}
