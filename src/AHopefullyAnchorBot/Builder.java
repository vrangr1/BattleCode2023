package AHopefullyAnchorBot;

import battlecode.common.*;

public class Builder extends Utils {
    private static int headquarterIndex = -1;
    public static int adamantium, mana, elixir;
    private static final boolean USING_CWBUILDER = true;

    public static void initBuilder() throws GameActionException{
        adamantium = 0;
        mana = 0;
        elixir = 0;
        if (USING_CWBUILDER)
            CWBuilder.initCWBuilder();
    }

    private static void updateBuilder() throws GameActionException{
        adamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        mana = rc.getResourceAmount(ResourceType.MANA);
        elixir = rc.getResourceAmount(ResourceType.ELIXIR);
    }

    public static boolean hasResourcesToBuild(RobotType type, int count){
        switch(type){
            case CARRIER: return adamantium >= count * RobotType.CARRIER.getBuildCost(ResourceType.ADAMANTIUM);
            case LAUNCHER: return mana >= count * RobotType.LAUNCHER.getBuildCost(ResourceType.MANA);
            case BOOSTER: return elixir >= count * RobotType.BOOSTER.getBuildCost(ResourceType.ELIXIR);
            case DESTABILIZER: return elixir >= count * RobotType.DESTABILIZER.getBuildCost(ResourceType.ELIXIR);
            case AMPLIFIER: return (
                adamantium >= count * RobotType.CARRIER.getBuildCost(ResourceType.ADAMANTIUM)
                && 
                mana >= count * RobotType.AMPLIFIER.getBuildCost(ResourceType.MANA)
            );
            default: break;
        }
        assert false;
        return false;
    }

    public static boolean hasResourcesToBuild(Anchor anchor, int count){
        switch(anchor){
            case STANDARD: return (
                adamantium >= count * Anchor.STANDARD.adamantiumCost
                && 
                mana >= count * Anchor.STANDARD.manaCost
            );
            case ACCELERATING: return elixir >= count * Anchor.ACCELERATING.elixirCost;
            default: break;
        }
        assert false;
        return false;
    }

    private static void tryToBuild(RobotType robotType, MapLocation loc) throws GameActionException{
        if (rc.canBuildRobot(robotType, loc))
            rc.buildRobot(robotType, loc);
    }

    public static void setHeadquarterIndex(int index){
        headquarterIndex = index;
    }

    private static void tryToBuild(Anchor anchor) throws GameActionException{
        if (rc.canBuildAnchor(anchor)){
            rc.buildAnchor(anchor);
            MapLocation loc = Comms.readLocationFromMessage(rc.readSharedArray(headquarterIndex - 2));
            assert loc.equals(currentLocation) : "has to be";
            int num_anchors = rc.getNumAnchors(Anchor.STANDARD);
            assert num_anchors - 1 == Comms.readMessageWithoutSHAFlag(headquarterIndex) : "num anchor correctness";
            Comms.writeSHAFlagMessage(num_anchors, Comms.SHAFlag.COLLECT_ANCHOR, headquarterIndex);
        }
    }

    public static void sendAnchorCollectionCommand() throws GameActionException{
        MapLocation loc = Comms.readLocationFromMessage(rc.readSharedArray(headquarterIndex - 2));
        assert loc.equals(currentLocation) : "has to be";

        int numAnchors = rc.getNumAnchors(Anchor.STANDARD);
        assert numAnchors != 0 : "num anchor correctness";
        Comms.writeSHAFlagMessage(numAnchors, Comms.SHAFlag.COLLECT_ANCHOR, headquarterIndex);
    }

    public static void buildUnits() throws GameActionException{
        updateBuilder();
        if (USING_CWBUILDER){
            CWBuilder.buildUnits();
            return;
        }

        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation newLoc = rc.getLocation().add(dir);

        if (rc.getRoundNum() % 30 == 0){
            rc.setIndicatorString("Trying to build a amplifier");
            tryToBuild(RobotType.AMPLIFIER, newLoc);
        } else if (rc.getRoundNum() % 40 == 0){
            rc.setIndicatorString("Trying to build a carrier");
            tryToBuild(RobotType.CARRIER, newLoc);
        } 
        else if (rc.getRoundNum() % 70 == 0){
            rc.setIndicatorString("Trying to build an anchor");
            tryToBuild(Anchor.STANDARD);
        }
        else{
            rc.setIndicatorString("Trying to build a launcher");
            tryToBuild(RobotType.LAUNCHER, newLoc);
        }
    }
}