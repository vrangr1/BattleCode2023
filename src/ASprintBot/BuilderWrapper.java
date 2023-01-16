package ASprintBot;

import battlecode.common.*;

public class BuilderWrapper extends Utils {
    private static int headquarterMessageIndex = -1;
    private static int headquarterSpawnIndex = -1;
    public static int adamantium, mana, elixir;
    private static boolean elixirWellFound = false;

    private enum BUILDERS{
        CWBUILDER,
        SIMPLEBUILDER,
        SAVVYBUILDER
    };

    private static final BUILDERS CURRENT_BUILDER = BUILDERS.SIMPLEBUILDER;

    public static void initBuilder() throws GameActionException{
        adamantium = 0;
        mana = 0;
        elixir = 0;
        switch(CURRENT_BUILDER){
            case CWBUILDER: CWBuilder.initBuilder(); break;
            case SAVVYBUILDER: SavvyBuilder.initBuilder(); break;
            case SIMPLEBUILDER: SimpleBuilder.initBuilder(); break;
            default: break;
        }
    }

    private static void updateBuilder() throws GameActionException{
        adamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        mana = rc.getResourceAmount(ResourceType.MANA);
        elixir = rc.getResourceAmount(ResourceType.ELIXIR);
        switch(CURRENT_BUILDER){
            case CWBUILDER: CWBuilder.updateBuilder(); break;
            case SAVVYBUILDER: SavvyBuilder.updateBuilder(); break;
            case SIMPLEBUILDER: SimpleBuilder.updateBuilder(); break;
            default: break;
        }
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

    public static void setHeadquarterIndex(int index){
        headquarterMessageIndex = index;
        headquarterSpawnIndex = (((index - 2) - Comms.START_CHANNEL_BANDS) / Comms.CHANNELS_COUNT_PER_HEADQUARTER) + 1;
    }

    public static int getHeadquarterSpawnIndex(){
        return headquarterSpawnIndex;
    }

    public static void sendAnchorCollectionCommand() throws GameActionException{
        MapLocation loc = Comms.readLocationFromMessage(rc.readSharedArray(headquarterMessageIndex - 2));
        assert loc.equals(currentLocation) : "has to be";

        int numAnchors = rc.getNumAnchors(Anchor.STANDARD);
        assert numAnchors != 0 : "num anchor correctness";
        Comms.writeSHAFlagMessage(numAnchors, Comms.SHAFlag.COLLECT_ANCHOR, headquarterMessageIndex);
    }

    public static ResourceType getPrioritizedResource(){
        return (rc.getRoundNum() % 2 == 0) ? ResourceType.ADAMANTIUM : ResourceType.MANA;
    }

    public static ResourceType mostNeedOfResource() throws GameActionException{
        // if ()
        if (Math.abs(adamantium - mana) > 500){
            if (adamantium > mana) return ResourceType.MANA;
            return ResourceType.ADAMANTIUM;
        }
        if(elixirWellFound && elixir < 100) return ResourceType.ELIXIR;
        return null;
    }

    public static MapLocation findBestSpawnLocationForCarrier() throws GameActionException{
        MapLocation bestLocation = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Direction dir : directions){
            MapLocation loc = currentLocation.add(dir);
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) continue;
            int distance = currentLocation.distanceSquaredTo(loc);
            if (distance < bestDistance){
                bestDistance = distance;
                bestLocation = loc;
            }
        }
        return bestLocation;
    }

    public static MapLocation findBestSpawnLocation(RobotType robotType) throws GameActionException{
        switch(robotType){
            case CARRIER: return findBestSpawnLocationForCarrier();
            case LAUNCHER: return findBestSpawnLocationForCarrier();
            case BOOSTER: return findBestSpawnLocationForCarrier();
            case DESTABILIZER: return findBestSpawnLocationForCarrier();
            case AMPLIFIER: return findBestSpawnLocationForCarrier();
            default: break;
        }
        assert false;
        return null;
    }

    public static void buildUnits() throws GameActionException{
        updateBuilder();
        switch(CURRENT_BUILDER){
            case CWBUILDER: CWBuilder.buildUnits(); break;
            case SAVVYBUILDER: SavvyBuilder.buildUnits(); break;
            case SIMPLEBUILDER: SimpleBuilder.buildUnits(); break;
            default: break;
        }
    }
}
