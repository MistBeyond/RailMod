package com.mistbeyond.rail.client.rail.model;

import com.mistbeyond.rail.api.rail.graph.GridDirection;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailSignalBlockModelTest {
    /**
     * The fixed template geometry: base (2), pole (1), red head (5) and green head (5).
     */
    private static final int TEMPLATE_ELEMENT_COUNT = 13;

    @Test
    void facingAnglesRotateTheNorthTemplateTowardsTheSignalDirection() {
        // The template faces north; joml rotationY(90) turns north into west, so the mapping follows the
        // 45-degree compass steps with EAST at 270 degrees.
        assertEquals(0.0F, RailSignalBlockModel.facingDegrees(GridDirection.NORTH), 1.0E-6F);
        assertEquals(45.0F, RailSignalBlockModel.facingDegrees(GridDirection.NORTH_WEST), 1.0E-6F);
        assertEquals(90.0F, RailSignalBlockModel.facingDegrees(GridDirection.WEST), 1.0E-6F);
        assertEquals(135.0F, RailSignalBlockModel.facingDegrees(GridDirection.SOUTH_WEST), 1.0E-6F);
        assertEquals(180.0F, RailSignalBlockModel.facingDegrees(GridDirection.SOUTH), 1.0E-6F);
        assertEquals(225.0F, RailSignalBlockModel.facingDegrees(GridDirection.SOUTH_EAST), 1.0E-6F);
        assertEquals(270.0F, RailSignalBlockModel.facingDegrees(GridDirection.EAST), 1.0E-6F);
        assertEquals(315.0F, RailSignalBlockModel.facingDegrees(GridDirection.NORTH_EAST), 1.0E-6F);
    }

    @Test
    void facingRotationsCoverTheFullCircle() {
        // Every facing must map onto the circle at exact 45-degree steps so opposite signals face each other.
        for (GridDirection facing : GridDirection.values()) {
            float degrees = RailSignalBlockModel.facingDegrees(facing);
            assertEquals(0.0F, degrees % 45.0F, 1.0E-4F, facing + " must map to a 45-degree step");
        }
    }

    @Test
    void templateGeometryIsMirroredCompletely() {
        assertEquals(TEMPLATE_ELEMENT_COUNT, RailSignalBlockModel.ELEMENTS.size());
    }

    @Test
    void lensFacetsKeepTheirTemplateTilts() {
        List<RailSignalBlockModel.Element> elements = RailSignalBlockModel.ELEMENTS;
        long yTilts = elements.stream()
                .filter(element -> element.tilt() != null && element.tilt().axis() == Direction.Axis.Y)
                .count();
        long zTilts = elements.stream()
                .filter(element -> element.tilt() != null && element.tilt().axis() == Direction.Axis.Z)
                .count();
        // The octagonal housing facets tilt about Y, the lens facets about Z, exactly like the template.
        assertEquals(2, yTilts);
        assertEquals(2, zTilts);
        for (RailSignalBlockModel.Tilt tilt : elements.stream().map(RailSignalBlockModel.Element::tilt).toList()) {
            if (tilt != null) {
                assertEquals(45.0F, tilt.angleDegrees(), 1.0E-6F);
            }
        }
    }

    @Test
    void baseAndPoleHaveNoTilt() {
        List<RailSignalBlockModel.Element> elements = RailSignalBlockModel.ELEMENTS;
        for (int i = 0; i <= 2; i++) {
            assertNull(elements.get(i).tilt(), "base and pole elements must be axis-aligned");
        }
        // The whole template stands on the ground plane and stays inside one block's height.
        assertTrue(elements.stream().allMatch(element -> element.fromY() >= 0.0F));
        assertTrue(elements.stream().allMatch(element -> element.toY() <= 16.0F));
    }

    @Test
    void lensesAndVisorsAreOnTheNorthFace() {
        // The template faces north: every lens element sits in the northern half (z below the block center).
        for (RailSignalBlockModel.Element element : RailSignalBlockModel.ELEMENTS) {
            if (element.lens() == RailSignalBlockModel.Lens.RED
                    || element.lens() == RailSignalBlockModel.Lens.GREEN
                    || element.lens() == RailSignalBlockModel.Lens.TRIM) {
                assertTrue(element.toZ() <= 8.0F, element + " must face north");
            }
        }
    }
}
