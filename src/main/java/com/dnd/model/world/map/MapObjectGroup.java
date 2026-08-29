package com.dnd.model.world.map;

import java.util.ArrayList;
import java.util.List;

/**
 * A named grouping of map objects in the Map Editor's object tree - {@link MapLayer}s,
 * {@link Drawing}s, and/or other {@code MapObjectGroup}s - so the DM can move, select, or edit
 * several related objects together (e.g. all the shapes making up one room outline).
 *
 * <p>{@link #memberKeys} references members by a prefixed key rather than embedding them
 * directly, since a member can be any of three different sibling lists on {@link GameMap}
 * ({@code layers}, {@code drawings}, {@code groups}):</p>
 * <ul>
 *   <li>{@code "layer:<layerId>"}</li>
 *   <li>{@code "drawing:<drawingId>"}</li>
 *   <li>{@code "group:<groupId>"} (nested group)</li>
 * </ul>
 */
public class MapObjectGroup {
    private String id;
    private String label;
    private List<String> memberKeys = new ArrayList<>();
    /** Stacking order shared with {@link MapLayer}/{@link Drawing}; higher draws on top. */
    private int zOrder = 0;

    public MapObjectGroup() {
    }

    public MapObjectGroup(String id, String label, List<String> memberKeys) {
        this.id = id;
        this.label = label;
        this.memberKeys = memberKeys != null ? memberKeys : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public List<String> getMemberKeys() { return memberKeys; }
    public void setMemberKeys(List<String> memberKeys) { this.memberKeys = memberKeys != null ? memberKeys : new ArrayList<>(); }

    public int getZOrder() { return zOrder; }
    public void setZOrder(int zOrder) { this.zOrder = zOrder; }
}
