package com.dnd.data.dto;

import com.dnd.model.world.map.GameMap;

import java.util.List;

/**
 * JSON wrapper for a campaign's {@code world/maps.json} file.
 * Each entry is a full {@link GameMap} (grid layout, occupants, etc.).
 */
public class MapCatalog {
    private List<GameMap> maps;

    public MapCatalog() {
    }

    public List<GameMap> getMaps() {
        return maps;
    }

    public void setMaps(List<GameMap> maps) {
        this.maps = maps;
    }
}

