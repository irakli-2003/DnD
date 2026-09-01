package com.dnd.model.world.map;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Works out how far a creature can move across a {@link GameMap} this turn.
 *
 * <p>Movement is costed per square entered, so terrain matters: normal ground
 * costs one, difficult ground costs two, and water or climbable rock cost one to
 * a creature with the matching speed but the difficult rate to anything that has
 * to improvise. Walls are never crossed, and a square already holding another
 * creature is treated as blocked so tokens cannot be stacked by accident.</p>
 *
 * <p>Diagonals cost the same as orthogonal steps, which is the common tabletop
 * simplification and keeps the reachable area predictable at the table.</p>
 */
public final class MovementCalculator {

    /** Marker in the cost grid for a square this creature cannot reach this turn. */
    public static final int UNREACHABLE = -1;

    private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DY = {0, 0, 1, -1, 1, -1, 1, -1};

    private MovementCalculator() {
    }

    /**
     * Costs every square reachable from the token's current position within
     * {@code budget} squares of movement.
     *
     * @return a {@code [height][width]} grid holding the cheapest cost to reach each
     *         square, or {@link #UNREACHABLE}. The starting square costs zero.
     */
    public static int[][] reachableFrom(GameMap map, MapObject token, int budget) {
        int[][] cost = new int[map.getHeight()][map.getWidth()];
        for (int[] row : cost) {
            java.util.Arrays.fill(row, UNREACHABLE);
        }
        Position start = token == null ? null : token.getPosition();
        if (start == null || budget < 0) return cost;
        if (!inBounds(map, start.getX(), start.getY())) return cost;

        CombatState state = TokenSupport.combatOf(token);
        boolean climbs = state.hasClimbSpeed();
        boolean swims = state.hasSwimSpeed();

        cost[start.getY()][start.getX()] = 0;

        // A plain queue is enough despite the varying costs: the grid is small and
        // re-visiting a square is only allowed when we found a strictly cheaper route,
        // so this settles to the true minimum without a priority queue's overhead.
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{start.getX(), start.getY()});

        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            int x = at[0];
            int y = at[1];
            int soFar = cost[y][x];

            for (int dir = 0; dir < DX.length; dir++) {
                int nx = x + DX[dir];
                int ny = y + DY[dir];
                if (!inBounds(map, nx, ny)) continue;

                GridCell cell = map.getCell(nx, ny);
                if (!cell.isPassable() || holdsOtherCreature(cell, token)) continue;

                TerrainType terrain = cell.getTerrain() == null ? TerrainType.NORMAL : cell.getTerrain();
                int next = soFar + terrain.costFor(climbs, swims);
                if (next > budget) continue;
                if (cost[ny][nx] != UNREACHABLE && cost[ny][nx] <= next) continue;

                cost[ny][nx] = next;
                queue.add(new int[]{nx, ny});
            }
        }
        return cost;
    }

    /**
     * Cost for {@code token} to reach {@code (x, y)} this turn, or {@link #UNREACHABLE}
     * when the square is out of range, walled off, or occupied.
     */
    public static int costToEnter(GameMap map, MapObject token, int x, int y, int budget) {
        if (!inBounds(map, x, y)) return UNREACHABLE;
        return reachableFrom(map, token, budget)[y][x];
    }

    private static boolean holdsOtherCreature(GridCell cell, MapObject mover) {
        for (MapObject occupant : cell.getOccupants()) {
            if (occupant != mover && TokenSupport.isCreature(occupant)) return true;
        }
        return false;
    }

    private static boolean inBounds(GameMap map, int x, int y) {
        return x >= 0 && y >= 0 && x < map.getWidth() && y < map.getHeight();
    }
}
