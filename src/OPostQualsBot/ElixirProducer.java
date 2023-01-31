package OPostQualsBot;

import battlecode.common.*;

public class ElixirProducer extends Utils {
    private static final boolean DOING_ELIXIR_PRODUCTION = false;
    private static MapLocation elixirTarget = null;
    private static boolean isElixirWellMade = false;
    public static final int ADAMANTIUM_DEPOSITION_FOR_ELIXIR_WELL_CREATION = 40;
    public static boolean goingToElixirWell = false;
    public static final int ELIXIR_TO_MANA_RATIO = 1;
    public static final int ADAMANTIUM_CARRIERS_RATIO_TO_MAKE_ELIXIR_WELL = 4;


    ////////////////////////////////////////
    // METHODS FOR ALL UNITS ///////////////
    ////////////////////////////////////////

    public static boolean shouldProduceElixir() {
        return DOING_ELIXIR_PRODUCTION && MAP_SIZE > 1000 && rc.getRoundNum() > 400;
    }



    ////////////////////////////////////////
    // METHODS FOR HEADQUARTERS ////////////
    ////////////////////////////////////////

    public static void updateElixirStuff() throws GameActionException{
        if (!shouldProduceElixir()) return;
        if (elixirTarget == null) setManaWellToConvert();
    }

    private static void setManaWellToConvert() throws GameActionException{
        assert UNIT_TYPE == RobotType.HEADQUARTERS;
        if (elixirTarget != null) return;
        elixirTarget = Comms.getElixirWellTarget();
        if (elixirTarget != null) return;
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
        currentLocation = rc.getLocation();
        return (currentLocation.distanceSquaredTo(hqLoc) > currentLocation.distanceSquaredTo(loc) + 36);
    }
}