package FirstBot;

import battlecode.common.*;

import java.util.Random; // TODO: Remove this. Or check bytecode cost of methods.

public class Utils extends Constants{
    static final Random rng = new Random(6147);

    public static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
}
