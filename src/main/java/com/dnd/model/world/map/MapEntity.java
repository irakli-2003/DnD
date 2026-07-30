package com.dnd.model.world.map;

/**
 * A placeable map marker that references an existing catalog entity by id
 * (e.g. an NPC, monster, item, or beast), the same way {@code PlayerCharacter}
 * references {@code classId}/{@code raceId} rather than embedding full
 * objects. This keeps map placement decoupled from the referenced entity's
 * own lifecycle - moving/removing a token on the map never mutates the
 * underlying catalog entry.
 */
public class MapEntity implements MapObject {
    private String id;
    private String name;
    /** Discriminator for {@link MapObject} polymorphic (de)serialization; always "entity" for this type. */
    private String kind = "entity";
    /** Which catalog the referenced entity belongs to, e.g. "npc", "monster", "item", "beast". */
    private String entityType;
    /** Id of the referenced entity within that catalog. */
    private String entityId;
    private String symbol;
    private Position position;

    public MapEntity() {
    }

    public MapEntity(String id, String name, String entityType, String entityId, String symbol, Position position) {
        this.id = id;
        this.name = name;
        this.entityType = entityType;
        this.entityId = entityId;
        this.symbol = symbol;
        this.position = position;
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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String getSymbol() {
        return symbol != null && !symbol.isEmpty() ? symbol : "?";
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public String toString() {
        String label = name != null ? name : entityId;
        return label + " [" + getSymbol() + "]" + (position != null ? " @ " + position : "");
    }
}

