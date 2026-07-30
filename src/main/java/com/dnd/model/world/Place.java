package com.dnd.model.world;

import com.dnd.model.creature.Habitat;
import com.dnd.model.interfaces.Printable;

import java.util.List;
import java.util.Map;

public class Place implements Printable {
    private String id;
    private String name;
    private String description;
    private String type;
    private Habitat habitat;
    private List<String> tags;
    private Map<String, String> attributes;
    /**
     * Id of the {@link com.dnd.model.world.map.GameMap} that belongs to this
     * place.  {@code null} means no map has been created for this place yet.
     * The map itself is stored in the campaign's {@code world/maps.json} file
     * and looked up via {@code CampaignRepositories.maps().getById(mapId)}.
     */
    private String mapId;

    public Place() {
    }

    public Place(String id, String name, String description, String type, Habitat habitat, List<String> tags, Map<String, String> attributes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.habitat = habitat;
        this.tags = tags;
        this.attributes = attributes;
    }

    public Place(String id, String name, String description, String type, Habitat habitat, List<String> tags, Map<String, String> attributes, String mapId) {
        this(id, name, description, type, habitat, tags, attributes);
        this.mapId = mapId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Habitat getHabitat() {
        return habitat;
    }

    public void setHabitat(Habitat habitat) {
        this.habitat = habitat;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}
