package com.dnd.model.world.map;

import java.util.ArrayList;
import java.util.List;

/**
 * A free-form annotation drawn directly on a {@link GameMap} by the DM (e.g. a highlighted
 * room outline, a marked area of effect, a hand-drawn note) - independent of the {@link
 * MapLayer} background images and the {@link MapObject} tokens placed in {@link GridCell}s.
 *
 * <p>{@link #points} are stored in grid-cell units (not pixels), so the drawing stays correctly
 * positioned regardless of the on-screen cell size used to render it.</p>
 */
public class Drawing {

    /** The kind of shape {@link #points} describes. */
    public enum Type { FREEHAND, LINE, RECTANGLE, OVAL }

    private String id;
    private Type type = Type.FREEHAND;
    private String color = "#c9a84c";
    private double lineWidth = 2.0;
    private boolean filled = false;
    /** Flat list of grid-space coordinates: {@code x0, y0, x1, y1, ...}. */
    private List<Double> points = new ArrayList<>();

    public Drawing() {
    }

    public Drawing(String id, Type type, String color, double lineWidth, boolean filled, List<Double> points) {
        this.id = id;
        this.type = type;
        this.color = color;
        this.lineWidth = lineWidth;
        this.filled = filled;
        this.points = points != null ? points : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public double getLineWidth() { return lineWidth; }
    public void setLineWidth(double lineWidth) { this.lineWidth = lineWidth; }

    public boolean isFilled() { return filled; }
    public void setFilled(boolean filled) { this.filled = filled; }

    public List<Double> getPoints() { return points; }
    public void setPoints(List<Double> points) { this.points = points != null ? points : new ArrayList<>(); }
}
