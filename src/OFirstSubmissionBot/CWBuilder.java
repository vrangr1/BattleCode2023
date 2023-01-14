package OFirstSubmissionBot;

import battlecode.common.*;

// Ivan Geffner's code. All hail Ivan Geffner!

public class CWBuilder extends Utils{
    private static int minLauncherScore = 100; //TODO: Tune this

    private static int carrierScore;
    private static int launcherScore;
    private static int amplifierScore;
    private static int boosterScore;
    private static int destabilizerScore;
    private static int standardAnchorScore;
    private static int acceleratingAnchorScore;
    private static Integer minCarriers = null;
    private static Integer minLaunchers = null;
    private static final int INITIAL_CARRIERS = 20;
    private static int lastBuildTurn = 0;
    private static int lastCarrierBuilt = 0;
    private static final int MIN_ADAMANTIUM_STOP_CARRIERS = 500;
    private static final int MIN_WAIT_CARRIERS = 15;
    private static final double MISCOUNT_FACTOR = 5;

    static class BuildRobotLoc {

        MapLocation loc;
        // Direction dir;
        boolean passable;
        int distToTarget;
        boolean canBuild;

        BuildRobotLoc(RobotType robotType, MapLocation spawnLoc, MapLocation target) {
            // this(robotType, currentLocation.directionTo(spawnLoc), target);
            this.canBuild = rc.canBuildRobot(robotType, spawnLoc);
            try {
                if (canBuild) {
                    this.loc = spawnLoc;
                    // this.dir = currentLocation.directionTo(spawnLoc);
                    this.passable = rc.sensePassability(spawnLoc);
                    if (target != null) distToTarget = loc.distanceSquaredTo(target);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        // TODO: Make this work
        // boolean isDangerous(){
        //     return minDistAttackers[dir.ordinal()] <= 20;
        // }

        boolean isBetterThan(BuildRobotLoc brl){
            if (!this.canBuild) return false;
            if (brl == null || !brl.canBuild) return true;
            if (this.passable && !brl.passable) return true;
            if (!this.passable) return false;

            // TODO: Make this work too...
            // if (!isDangerous() && brl.isDangerous()) return true;
            // if (isDangerous() && !brl.isDangerous()) return false;

            // if (isDangerous()){
            //     return minDistAttackers[dir.ordinal()] > minDistAttackers[brl.dir.ordinal()];
            // }

            return distToTarget < brl.distToTarget;
        }

    }

    public static int getMinCarriers(){
        if (minCarriers != null) return minCarriers;
        minCarriers = (MAP_HEIGHT * MAP_WIDTH)/200;
        if (minCarriers > INITIAL_CARRIERS) minCarriers = INITIAL_CARRIERS;
        return minCarriers;
    }

    public static int getMinLaunchers(){
        if (minLaunchers != null) return minLaunchers;
        minLaunchers = 0;
        return minLaunchers;
    }

    public static void initCWBuilder(){
        carrierScore = 0;
        launcherScore = 0;
        amplifierScore = 0;
        boosterScore = 0;
        destabilizerScore = 0;
        standardAnchorScore = 0;
        acceleratingAnchorScore = 0;
        if (minLauncherScore > getMinCarriers()) minLauncherScore = getMinCarriers();
    }

    private static void updateCWBuilder() throws GameActionException{
        launcherScore = Math.max(Comms.getRobotScore(RobotType.LAUNCHER), minLauncherScore);
        carrierScore = Comms.getRobotScore(RobotType.CARRIER);
        // amplifierScore = Comms.getRobotScore(RobotType.AMPLIFIER);
        // boosterScore = Comms.getRobotScore(RobotType.BOOSTER);
        // destabilizerScore = Comms.getRobotScore(RobotType.DESTABILIZER);
        standardAnchorScore = Comms.getRobotScore(Anchor.STANDARD);
        // acceleratingAnchorScore = Comms.getRobotScore(Anchor.ACCELERATING);
    }

    private static boolean constructRobotGreedy(RobotType t, MapLocation target){
        try {
            BuildRobotLoc bestBRL = null;

            for (Direction d : directions) {
                BuildRobotLoc brl = new BuildRobotLoc(t, rc.adjacentLocation(d), target);
                if (brl.isBetterThan(bestBRL)) bestBRL = brl;
            }
            if (bestBRL != null){
                if (rc.canBuildRobot(t, bestBRL.loc)) rc.buildRobot(t, bestBRL.loc);
                return true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private static boolean tryConstructEnvelope(RobotType t, MapLocation target){
        if (constructRobotGreedy(t, target)){
            lastBuildTurn = rc.getRoundNum();
            if (t == RobotType.CARRIER) lastCarrierBuilt = rc.getRoundNum();
            return true;
        }
        return false;
    }

    private static int updateCarrierScore(int oldScore){
        if (oldScore < CWBuilder.getMinCarriers()) return oldScore + 1;
        return oldScore + 3;
    }

    private static int updateLauncherScore(int oldScore){
        if (oldScore < CWBuilder.getMinCarriers()){
            if (oldScore + 3 > CWBuilder.getMinCarriers()) return CWBuilder.getMinCarriers();
            return oldScore + 3;
        }
        return oldScore + 1;
    }

    private static int updateAmplifierScore(int oldScore){
        if (oldScore < CWBuilder.getMinCarriers()){
            if (oldScore + 3 > CWBuilder.getMinCarriers()) return CWBuilder.getMinCarriers();
            return oldScore + 3;
        }
        return oldScore + 1;
    }

    private static int updateStandardAnchorScore(int oldScore) {
        return oldScore + 100;
    }
    
    private static int updateScore(RobotType type, int oldScore){
        switch(type){
            case CARRIER: return updateCarrierScore(oldScore);
            case LAUNCHER: return updateLauncherScore(oldScore);
            case AMPLIFIER: return updateAmplifierScore(oldScore);
            // case BOOSTER: return updateBoosterScore(oldScore);
            // case DESTABILIZER: return updateDestabilizerScore(oldScore);
            default: break;
        }
        assert false;
        return -1;
    }

    private static int updateScore(Anchor anchor, int oldScore){
        switch(anchor){
            case STANDARD: return updateStandardAnchorScore(oldScore);
            // case ACCELERATING: return updateAcceleratingAnchorScore(oldScore);
            default: break;
        }
        assert false;
        return -1;
    }

    private static boolean tryBuildLauncher() throws GameActionException{
        // if (Comms.getRobotCount(RobotType.LAUNCHER) >= getMinLaunchers()) return false;

        if (!Builder.hasResourcesToBuild(RobotType.LAUNCHER, 2)) return false;

        if (tryConstructEnvelope(RobotType.LAUNCHER, Comms.findNearestEnemyHeadquarterLocation())){
            Comms.writeScore(RobotType.LAUNCHER, updateScore(RobotType.LAUNCHER, launcherScore));
            return true;
        }
        return false;
    }

    private static boolean tryBuildCarrier() throws GameActionException{

        if (Builder.adamantium >= MIN_ADAMANTIUM_STOP_CARRIERS) {
            if (carrierScore <= launcherScore) {
                //we don't build it if too much lead
                Comms.writeScore(RobotType.CARRIER, updateScore(RobotType.CARRIER, carrierScore));
            }
            return false;
        }

        if ((
            // explore.cumulativeLead < 50 
            // || 
            rc.getRoundNum() - lastBuildTurn <= MIN_WAIT_CARRIERS) && carrierScore > launcherScore) return false;

        if (!Builder.hasResourcesToBuild(RobotType.CARRIER, 1)) return false;

        if (tryConstructEnvelope(RobotType.CARRIER, Comms.findNearestLocationOfThisType(currentLocation, Comms.COMM_TYPE.WELLS, Comms.SHAFlag.WELL_LOCATION))) {
            Comms.writeScore(RobotType.CARRIER, updateScore(RobotType.CARRIER, carrierScore));
            return true;
        }

        return false;
    }

    private static boolean shouldBuildAnchor() throws GameActionException{
        return (int)((double)Comms.getRobotCount(RobotType.LAUNCHER) * MISCOUNT_FACTOR) >= getMinLaunchers();
    }

    private static boolean tryBuildStandardAnchor() throws GameActionException{
        // if (!shouldBuildAnchor()) return false;
        // if (comm.builderAlive()) return false;

        if (!Builder.hasResourcesToBuild(Anchor.STANDARD, 1) || !Builder.hasResourcesToBuild(RobotType.LAUNCHER, 1)) return false;

        System.out.println("trying to build standard anchor");
        if (rc.canBuildAnchor(Anchor.STANDARD)){
            System.out.println("built standard anchor");
            rc.buildAnchor(Anchor.STANDARD);
            Comms.writeScore(Anchor.STANDARD, updateScore(Anchor.STANDARD, standardAnchorScore));
            Builder.sendAnchorCollectionCommand();
            return true;
        }
        return false;
    }

    public static boolean tryBuildAmplifier() throws GameActionException{
        if (!Builder.hasResourcesToBuild(RobotType.AMPLIFIER, 2)) return false;
        
        if (tryConstructEnvelope(RobotType.AMPLIFIER, Comms.findNearestEnemyHeadquarterLocation())){
            Comms.writeScore(RobotType.AMPLIFIER, updateScore(RobotType.AMPLIFIER, amplifierScore));
            return true;
        }
        return false;
    }

    public static void buildUnits() throws GameActionException{
        // if (rc.getMode() != RobotMode.TURRET) return;
        // mainArchon = comm.isMain();

        // rc.setIndicatorString(comm.getMainArchon() + " " + comm.myArchonIndex);

        // if (endangered()){
        //     comm.resetMain();
        //     mainArchon = true;

        //     tryConstructSage();
        //     tryBuildSoldier();
        // }

        updateCWBuilder();

        if (tryBuildCarrier()) return;
        if (tryBuildLauncher()) return;
        if (tryBuildStandardAnchor()) return;
        if (tryBuildAmplifier()) return;
        // if (tryBuildAcceleratingAnchor()) return;
        // if (tryBuildBooster()) return;
        // if (tryBuildDestabilizer()) return;
    }
}