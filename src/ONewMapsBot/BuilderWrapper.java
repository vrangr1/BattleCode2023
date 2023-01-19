package ONewMapsBot;

import battlecode.common.*;

public class BuilderWrapper extends Utils {
    private static int headquarterMessageIndex = -1;
    private static int headquarterSpawnIndex = -1;
    public static int adamantium, mana, elixir;
    private static boolean elixirWellFound = false;
    private static ResourceType prioritizedResource;
    private static ResourceType writePrioritizedResource = ResourceType.NO_RESOURCE;
    private static boolean carrierBuilt = true;
    private static final double RESOURCE_MANA_ADAMANTIUM_RATIO = 1;
    private static final double RESOURCE_ELIXIR_NORMAL_RATIO = 1;
    private static int carrierResourceCount = 0;

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
        prioritizedResource = ResourceType.ADAMANTIUM;
        writePrioritizedResource = ResourceType.NO_RESOURCE;
        carrierBuilt = true;
        carrierResourceCount = 0;
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
        if (writePrioritizedResource != ResourceType.NO_RESOURCE){
            assert rc.getRoundNum() > 1 : "round num correctness";
            assert headquarterMessageIndex != -1 : "headquarter message index correctness";
            assert carrierBuilt : "carrier built correctness";
            Comms.writePrioritizedResource(writePrioritizedResource, headquarterMessageIndex);
            writePrioritizedResource = ResourceType.NO_RESOURCE;
        }
        else if (!carrierBuilt){
            // updatePrioritizedResource();
            rollBackPrioritizedResource();
            carrierBuilt = true;
        }
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

    // Resource Prioritization

    private static void rollBackPrioritizedResource(){
        carrierResourceCount--;
        if (carrierResourceCount < 0) carrierResourceCount = (int)(RESOURCE_MANA_ADAMANTIUM_RATIO);
        prioritizedResource = (carrierResourceCount < RESOURCE_MANA_ADAMANTIUM_RATIO) ? ResourceType.MANA : ResourceType.ADAMANTIUM;
    }

    private static void updatePrioritizedResource(){
        // prioritizedResource = (prioritizedResource == ResourceType.ADAMANTIUM) ? ResourceType.MANA : ResourceType.ADAMANTIUM;
        prioritizedResource = (carrierResourceCount < RESOURCE_MANA_ADAMANTIUM_RATIO) ? ResourceType.MANA : ResourceType.ADAMANTIUM;
        carrierResourceCount = (carrierResourceCount + 1) % (int)(RESOURCE_MANA_ADAMANTIUM_RATIO+1);
    }

    public static void setPrioritizedResource(ResourceType resource){
        carrierBuilt = true;
        writePrioritizedResource = prioritizedResource;
    }

    public static ResourceType getPrioritizedResource(){
        carrierBuilt = false;
        updatePrioritizedResource();
        return prioritizedResource;
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

    private static MapLocation findNearestWellForCarrier(ResourceType pResourceType) throws GameActionException{
        currentLocation = rc.getLocation();
        MapLocation senseLoc = BotCarrier.findNearestWellInVision(prioritizedResource);
        if (senseLoc != null) return senseLoc;
        MapLocation commsLoc = Comms.findNearestLocationOfThisType(currentLocation, Comms.COMM_TYPE.WELLS, Comms.resourceFlag(pResourceType));
        if (commsLoc != null) return commsLoc;
        return null;
    }

    private static MapLocation findBestSpawnLocationForCarrier(ResourceType pResourceType) throws GameActionException{
        MapLocation targetLoc = findNearestWellForCarrier(pResourceType);
        if (targetLoc == null){
            // TODO: Do something
            return null;
        }
        if (rc.canBuildRobot(RobotType.CARRIER, targetLoc)) return targetLoc;
        return findNearestActReadyLocation(targetLoc, RobotType.CARRIER);
    }

    private static MapLocation findBestSpawnLocationForLauncher() throws GameActionException{
        return findNearestActReadyLocation(returnEnemyHQGuess(), RobotType.LAUNCHER);
    }

    private static MapLocation findBestSpawnLocationForBooster() throws GameActionException{
        return null;
    }

    private static MapLocation findBestSpawnLocationForAmplifier() throws GameActionException{
        return null;
    }

    private static MapLocation findBestSpawnLocationForDestabilizer() throws GameActionException{
        return null;
    }

    public static MapLocation findBestSpawnLocation(RobotType robotType) throws GameActionException{
        switch(robotType){
            case CARRIER: assert false; break;
            case LAUNCHER: return findBestSpawnLocationForLauncher();
            case BOOSTER: return findBestSpawnLocationForBooster();
            case DESTABILIZER: return findBestSpawnLocationForDestabilizer();
            case AMPLIFIER: return findBestSpawnLocationForAmplifier();
            default: break;
        }
        assert false;
        return null;
    }

    public static MapLocation findBestSpawnLocation(RobotType robotType, ResourceType pResourceType) throws GameActionException{
        switch(robotType){
            case CARRIER: return findBestSpawnLocationForCarrier(pResourceType);
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
