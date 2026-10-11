package com.mistbeyond.rail.core.rail;

import com.mistbeyond.rail.api.rail.dispatch.PathfindingOptions;
import com.mistbeyond.rail.api.rail.dispatch.RailRoute;
import com.mistbeyond.rail.api.rail.dispatch.RailTrainId;
import com.mistbeyond.rail.api.rail.dispatch.RouteRequest;
import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.graph.RailEdgeId;
import com.mistbeyond.rail.api.rail.graph.RailNodeId;
import com.mistbeyond.rail.api.rail.graph.TrackPlacement;
import com.mistbeyond.rail.api.rail.graph.TrackType;
import com.mistbeyond.rail.core.rail.RailEdge;
import com.mistbeyond.rail.core.rail.RailNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortestPathRouterTest {
    @Test
    void findsTwoHopRouteOnStraightLine() {
        RailNode a = node("a", 0);
        RailNode b = node("b", 1);
        RailNode c = node("c", 2);
        RailGraph graph = new RailGraph(
                Set.of(a, b, c),
                Set.of(
                        new RailEdge(
                                new RailEdgeId("a-b"),
                                a.id(),
                                b.id(),
                                new TrackPlacement(new GridPos(0, 0, 0), GridDirection.EAST, TrackType.STRAIGHT),
                                1.0
                        ),
                        new RailEdge(
                                new RailEdgeId("b-c"),
                                b.id(),
                                c.id(),
                                new TrackPlacement(new GridPos(1, 0, 0), GridDirection.EAST, TrackType.STRAIGHT),
                                1.0
                        )
                ),
                Set.of()
        );
        RouteRequest request = new RouteRequest(
                new RailTrainId("train-1"),
                a.id(),
                c.id(),
                List.of(),
                PathfindingOptions.DEFAULT
        );

        Optional<RailRoute> route = new ShortestPathRouter().findRoute(graph, request);

        assertTrue(route.isPresent());
        assertEquals(2, route.orElseThrow().steps().size());
        assertEquals(a.id(), route.orElseThrow().start());
        assertEquals(c.id(), route.orElseThrow().end());
    }

    private static RailNode node(String id, int x) {
        return new RailNode(new RailNodeId(id), new GridPos(x, 0, 0));
    }
}
