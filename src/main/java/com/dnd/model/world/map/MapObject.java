package com.dnd.model.world.map;

import com.dnd.model.interfaces.Printable;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Anything that can be placed on a {@link GameMap} grid: it has a
 * {@link Position} and a short {@link #getSymbol()} shown in its grid box
 * when the map is printed (its full {@link #toString()}/{@link #print()}
 * still shows the complete description, same as every other {@link Printable}).
 *
 * <p>New placeable types can be registered in {@code @JsonSubTypes} below
 * without touching {@link GridCell} or {@link GameMap}.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", defaultImpl = MapEntity.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = MapEntity.class,    name = "entity"),
    @JsonSubTypes.Type(value = MapItemToken.class, name = "item"),
    @JsonSubTypes.Type(value = PlayerToken.class,  name = "player"),
    @JsonSubTypes.Type(value = NpcToken.class,     name = "npc"),
    @JsonSubTypes.Type(value = MonsterToken.class, name = "monster"),
    @JsonSubTypes.Type(value = BeastToken.class,   name = "beast")
})
public interface MapObject extends Printable {
    Position getPosition();

    void setPosition(Position position);

    /**
     * Short symbol (typically a single character) displayed in this object's
     * grid box when {@link GameMap#print()} renders the map.
     */
    String getSymbol();
}

