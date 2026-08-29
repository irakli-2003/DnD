package com.dnd.model.world.map;

public class MapLayer {
    private String id;
    private String label;
    private String imagePath;
    /** Optional solid fill used when {@link #imagePath} is absent (e.g. a color-only layer). */
    private String fillColor;
    private double x = 0;
    private double y = 0;
    private double width = 10;
    private double height = 10;
    private int zOrder = 0;

    public MapLayer() {}
    public MapLayer(String id, String label, String imagePath,
                    double x, double y, double width, double height, int zOrder) {
        this.id = id; this.label = label; this.imagePath = imagePath;
        this.x = x; this.y = y; this.width = width; this.height = height;
        this.zOrder = zOrder;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getFillColor() { return fillColor; }
    public void setFillColor(String fillColor) { this.fillColor = fillColor; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public int getZOrder() { return zOrder; }
    public void setZOrder(int zOrder) { this.zOrder = zOrder; }
}
