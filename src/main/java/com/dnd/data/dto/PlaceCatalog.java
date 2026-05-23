package com.dnd.data.dto;

import com.dnd.model.world.Place;

import java.util.List;

public class PlaceCatalog {
    private List<Place> places;

    public PlaceCatalog() {
    }

    public List<Place> getPlaces() {
        return places;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
    }
}

