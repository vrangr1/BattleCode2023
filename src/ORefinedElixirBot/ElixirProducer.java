package ORefinedElixirBot;

import battlecode.common.*;

public class ElixirProducer extends Utils {
    private static final boolean DOING_ELIXIR_PRODUCTION = true;
    private static MapLocation elixirTarget = null, potentialElixirTarget = null;
    private static int potentialElixirTargetDistance = -1;
    private static boolean isElixirWellMade = false;
    public static final int ADAMANTIUM_DEPOSITION_FOR_ELIXIR_WELL_CREATION = 40;
    public static boolean goingToElixirWell = false;
    public static final int ELIXIR_TO_MANA_RATIO = 1;
    public static final int ADAMANTIUM_CARRIERS_RATIO_TO_MAKE_ELIXIR_WELL = 15;


    ////////////////////////////////////////
    // METHODS FOR ALL UNITS ///////////////
    ////////////////////////////////////////

    public static boolean shouldProduceElixir() throws GameActionException {
        return DOING_ELIXIR_PRODUCTION && MAP_SIZE > 1000 && (rc.getRoundNum() > 300 && rc.getRobotCount() > 20) && Comms.getHeadquartersCount() > 1;
    }



    ////////////////////////////////////////
    // METHODS FOR HEADQUARTERS ////////////
    ////////////////////////////////////////

    private static int getMinDistance(MapLocation loc, MapLocation[] locations, int count){
        int minDist = -1;
        for (int i = 0; i < count; ++i){
            if (minDist == -1 || loc.distanceSquaredTo(locations[i]) < minDist){
                minDist = loc.distanceSquaredTo(locations[i]);
            }
        }
        return minDist;
    }

    private static void surveyForPotentialElixirWellTargets() throws GameActionException{
        if (rc.getRoundNum() == 1) return;
        MapLocation[] alliedHQLocations = Comms.getAlliedHeadquartersLocationsList();
        int message, hqCount = Comms.getHeadquartersCount(), curDist;
        MapLocation loc;
        for (int i = Comms.COMM_TYPE.WELLS.channelStart; i < Comms.COMM_TYPE.WELLS.channelStop; ++i){
            message = rc.readSharedArray(i);
            if (Comms.readSHAFlagFromMessage(message) != Comms.SHAFlag.MANA_WELL_LOCATION) continue;
            loc = Comms.readLocationFromMessage(message);
            curDist = getMinDistance(loc, alliedHQLocations, hqCount);
            if (potentialElixirTarget == null || curDist < potentialElixirTargetDistance){
                potentialElixirTarget = loc;
                potentialElixirTargetDistance = getMinDistance(loc, alliedHQLocations, hqCount);
            }
        }
    }

    public static void updateElixirStuff() throws GameActionException{
        if (!shouldProduceElixir()){
            surveyForPotentialElixirWellTargets();
            return;
        }
        if (elixirTarget == null) setManaWellToConvert();
    }

    private static void setManaWellToConvert() throws GameActionException{
        assert UNIT_TYPE == RobotType.HEADQUARTERS;
        if (elixirTarget != null) return;
        elixirTarget = Comms.getElixirWellTarget();
        if (elixirTarget != null) return;
        elixirTarget = potentialElixirTarget;
        if (elixirTarget == null)
            elixirTarget = findNearestWellForCarrier(ResourceType.MANA);
        if (elixirTarget != null) Comms.writeElixirWellLocation(elixirTarget);
    }

    private static MapLocation findNearestWellForCarrier(ResourceType pResourceType) throws GameActionException{
        currentLocation = rc.getLocation();
        BotCarrier.otherTypeWell = null;
        MapLocation senseLoc = BotCarrier.findNearestWellInVision(pResourceType);
        if (senseLoc != null) return senseLoc;
        return Comms.findNearestLocationOfThisType(currentLocation, Comms.COMM_TYPE.WELLS, Comms.resourceFlag(pResourceType));
    }



    ////////////////////////////////////////
    // METHODS FOR CARRIER /////////////////
    ////////////////////////////////////////

    public static boolean checkIfElixirWellMade() throws GameActionException{
        if (!isElixirWellMade) isElixirWellMade = Comms.findIfElixirWellMade();
        return isElixirWellMade;
    }

    public static void setElixirWellMade() throws GameActionException{
        isElixirWellMade = true;
        Comms.saveOrWriteThatElixirWellMade();
    }

    /**
     * This method is called by the carrier to get the location of the mana well to convert to elixir.
     * @return
     * @throws GameActionException
     */
    public static MapLocation getManaWellToConvert() throws GameActionException {
        if (isElixirWellMade){
            assert elixirTarget != null;
            return elixirTarget;
        }
        if (elixirTarget != null) return elixirTarget;
        elixirTarget = Comms.getElixirWellTarget();
        return elixirTarget;
    }

    public static boolean rollTheDice(int chance) throws GameActionException{
        return (rc.getID() % (chance + 1)) < chance;
    }

    public static boolean shouldGoToWellForDeposit(MapLocation loc) throws GameActionException{
        if (loc == null) return false;
        MapLocation hqLoc = Comms.findNearestHeadquarter();
        return Comms.findNearestHeadquarter(loc).equals(hqLoc);
        // if (true) return true;
        // currentLocation = rc.getLocation();
        // return (currentLocation.distanceSquaredTo(hqLoc) + 16 > currentLocation.distanceSquaredTo(loc));
    }
}