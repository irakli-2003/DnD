package com.dnd.model.world.map;

/**
 * What a square is made of, for the purpose of moving across it.
 *
 * <p>This is deliberately separate from {@link GridCell#isPassable()}. Impassable
 * squares are walls - nothing crosses them by walking, climbing or swimming, so
 * they are not a terrain type. Terrain describes squares a creature <em>can</em>
 * enter, but at a price that depends on how the creature moves.</p>
 *
 * <p>Costs are in squares of movement spent to enter the square. {@code NORMAL}
 * and {@code DIFFICULT} follow the usual tabletop rule of one and two. Water and
 * climbable squares cost one to a creature with the matching speed and are
 * otherwise crossed at the difficult-terrain rate, which is how a creature
 * without a swim speed still gets to wade or scramble, just slowly.</p>
 */
public enum TerrainType {
    NORMAL("Normal", 1),
    DIFFICULT("Difficult", 2),
    WATER("Water", 1),
    CLIMB("Climb", 1);

    private final String label;
    private final int baseCost;

    TerrainType(String label, int baseCost) {
        this.label = label;
        this.baseCost = baseCost;
    }

    public String getLabel() {
        return label;
    }

    /** Cost to enter for a creature that has the movement mode this terrain calls for. */
    public int getBaseCost() {
        return baseCost;
    }

    /** True when crossing this square needs a swim speed to be done efficiently. */
    public boolean needsSwimSpeed() {
        return this == WATER;
    }

    /** True when crossing this square needs a climb speed to be done efficiently. */
    public boolean needsClimbSpeed() {
        return this == CLIMB;
    }

    /**
     * Movement cost to enter this square for a creature with the given speeds.
     *
     * <p>A creature with the right speed pays the base cost. Without it, it is
     * improvising - wading, scrambling - and pays the difficult-terrain cost.</p>
     */
    public int costFor(boolean hasClimbSpeed, boolean hasSwimSpeed) {
        if (needsSwimSpeed() && !hasSwimSpeed) return DIFFICULT.baseCost;
        if (needsClimbSpeed() && !hasClimbSpeed) return DIFFICULT.baseCost;
        return baseCost;
    }
}
