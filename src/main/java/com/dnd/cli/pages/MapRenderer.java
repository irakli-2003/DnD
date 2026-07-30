package com.dnd.cli.pages;

import com.dnd.model.world.map.GameMap;
import com.dnd.model.world.map.GridCell;
import com.dnd.model.world.map.Position;

import java.util.List;

/**
 * Renders a {@link GameMap} grid to a coordinate-labeled text block.
 *
 * <p>Row and column labels are plain decimal numbers, right-aligned to a
 * fixed width. Since each grid box is only one character wide, multi-digit
 * column numbers are split across stacked header rows - one digit per row,
 * most-significant first - so each digit lines up exactly above the column
 * it labels. For example, with 12 columns (indices 0-11):</p>
 *
 * <pre>
 *              111
 *    0123456789012
 *  0 ############
 *  1 #...#...#..#
 * </pre>
 *
 * <p>Shared by the DM's {@link MapSessionPage} (full visibility - no fog) and
 * the player's map page (fog-of-war limited to a vision radius around the
 * player's token), so the alignment logic only lives in one place.</p>
 */
public final class MapRenderer {
    /** Symbol shown for a grid box that's outside the viewer's vision radius. */
    private static final char FOG_SYMBOL = ' ';

    private MapRenderer() {
    }

    /** Renders the full grid with no fog-of-war (DM view - everything visible). */
    public static String renderFull(GameMap map) {
        return render(map, null, -1);
    }

    /**
     * Renders the grid with cells farther than {@code visionRadius} (Chebyshev
     * distance - matches diagonal movement counting as 1 square) from
     * {@code viewerPosition} replaced by blank fog boxes.
     */
    public static String renderWithFog(GameMap map, Position viewerPosition, int visionRadius) {
        return render(map, viewerPosition, visionRadius);
    }

    private static String render(GameMap map, Position viewerPosition, int visionRadius) {
        List<List<GridCell>> grid = map.getGrid();
        int width = map.getWidth();
        int height = grid.size();

        int rowLabelWidth = Math.max(String.valueOf(Math.max(height - 1, 0)).length(), 1);
        int colLabelWidth = Math.max(String.valueOf(Math.max(width - 1, 0)).length(), 1);
        String prefixBlank = repeat(' ', rowLabelWidth + 1);

        StringBuilder sb = new StringBuilder();
        if (map.getName() != null && !map.getName().isEmpty()) {
            sb.append(map.getName()).append("\n");
        }

        // Column header: one stacked row per digit position, most-significant first.
        for (int digitIndex = 0; digitIndex < colLabelWidth; digitIndex++) {
            sb.append(prefixBlank);
            for (int x = 0; x < width; x++) {
                String label = padLeft(String.valueOf(x), colLabelWidth);
                sb.append(label.charAt(digitIndex));
            }
            sb.append("\n");
        }

        // Grid rows, each prefixed with its right-aligned row label.
        for (int y = 0; y < height; y++) {
            sb.append(padLeft(String.valueOf(y), rowLabelWidth)).append(" ");
            List<GridCell> row = grid.get(y);
            for (int x = 0; x < row.size(); x++) {
                if (isVisible(viewerPosition, visionRadius, x, y)) {
                    sb.append(row.get(x).getDisplaySymbol());
                } else {
                    sb.append(FOG_SYMBOL);
                }
            }
            if (y < height - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private static boolean isVisible(Position viewerPosition, int visionRadius, int x, int y) {
        if (viewerPosition == null || visionRadius < 0) {
            return true;
        }
        int dx = Math.abs(x - viewerPosition.getX());
        int dy = Math.abs(y - viewerPosition.getY());
        return Math.max(dx, dy) <= visionRadius;
    }

    private static String padLeft(String value, int width) {
        if (value.length() >= width) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = value.length(); i < width; i++) {
            sb.append(' ');
        }
        sb.append(value);
        return sb.toString();
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}

