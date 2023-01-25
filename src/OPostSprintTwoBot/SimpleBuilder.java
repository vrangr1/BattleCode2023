package OPostSprintTwoBot;

import battlecode.common.*;

// Ivan Geffner's code. All hail Ivan Geffner!

public class SimpleBuilder extends Utils{
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
    // private static final int MIN_WAIT_AMPLIFIERS = 25;
    private static final double MISCOUNT_FACTOR = 5;
    public static final int INIT_ANCHOR_SCORE = 50;
    public static int anchorCountDown = 20;

    static class BuildRobotLoc {
        MapLocation loc;
        boolean passable;
        int distToTarget;
        boolean canBuild;

        BuildRobotLoc(RobotType robotType, MapLocation spawnLoc, MapLocation target) {
            this.canBuild = rc.canBuildRobot(robotType, spawnLoc);
            try {
                if (canBuild) {
                    this.loc = spawnLoc;
                    this.passable = rc.sensePassability(spawnLoc);
                    if (target != null) distToTarget = loc.distanceSquaredTo(target);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        boolean isBetterThan(BuildRobotLoc brl){
            if (!this.canBuild) return false;
            if (brl == null || !brl.canBuild) return true;
            if (this.passable && !brl.passable) return true;
            if (!this.passable) return false;
            return distToTarget < brl.distToTarget;
        }
    }

    public static int getMinCarriers(){
        if (minCarriers != null) return minCarriers;
        minCarriers = (MAP_HEIGHT * MAP_WIDTH)/125;
        if (minCarriers > INITIAL_CARRIERS) minCarriers = INITIAL_CARRIERS;
        return minCarriers;
    }

    public static int getMinLaunchers(){
        if (minLaunchers != null) return minLaunchers;
        minLaunchers = 0;
        return minLaunchers;
    }

    public static void initBuilder(){
        carrierScore = 0;
        launcherScore = 0;
        amplifierScore = 0;
        boosterScore = 0;
        destabilizerScore = 0;
        standardAnchorScore = 0;
        acceleratingAnchorScore = 0;
        if (minLauncherScore > getMinCarriers()) minLauncherScore = getMinCarriers();
    }

    public static void updateBuilder() throws GameActionException{
        launcherScore = Math.max(Comms.getRobotScore(RobotType.LAUNCHER), minLauncherScore);
        carrierScore = Comms.getRobotScore(RobotType.CARRIER);
        amplifierScore = Comms.getRobotScore(RobotType.AMPLIFIER);
        // boosterScore = Comms.getRobotScore(RobotType.BOOSTER);
        // destabilizerScore = Comms.getRobotScore(RobotType.DESTABILIZER);
        standardAnchorScore = Comms.getRobotScore(Anchor.STANDARD);
        // acceleratingAnchorScore = Comms.getRobotScore(Anchor.ACCELERATING);
    }

    private static boolean optimalBuildLocationUsage(RobotType robotType){
        switch(robotType){
            case CARRIER: return true;
            case LAUNCHER: return true;
            case AMPLIFIER: return false;
            case BOOSTER: return false;
            case DESTABILIZER: return false;
            default: return false;
        }
    }

    private static boolean constructRobotGreedy(RobotType t, MapLocation target){
        try {
            if (optimalBuildLocationUsage(t) && target != null){
                assert rc.canBuildRobot(t, target) : "round num : " + rc.getRoundNum() + " id: " + rc.getID() + " target: " + target + " curLoc: " + rc.getLocation();
                rc.buildRobot(t, target);
                return true;
            }
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
        if (oldScore < CWBuilder.getMinCarriers()) 
            return oldScore + 1;
        return oldScore + 3;
    }

    private static int updateLauncherScore(int oldScore){
        return oldScore + 1;
    }

    private static int updateAmplifierScore(int oldScore){
        return oldScore + 20;
    }

    private static int updateStandardAnchorScore(int oldScore) {
        return oldScore + 30;
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
        int soldierReq = 1;
        if (rc.getRobotCount() > MAP_SIZE / 4 || rc.getRobotCount() > 120 + MAP_SIZE/80 || rc.getRoundNum() >= 1700) soldierReq = 2;
        if (!BuilderWrapper.hasResourcesToBuild(RobotType.LAUNCHER, soldierReq)) {
            return false;
        }

        // if (tryConstructEnvelope(RobotType.LAUNCHER, Comms.findNearestEnemyHeadquarterLocation())){
        if (tryConstructEnvelope(RobotType.LAUNCHER, BuilderWrapper.findBestSpawnLocation(RobotType.LAUNCHER))){
            Comms.writeScore(RobotType.LAUNCHER, updateScore(RobotType.LAUNCHER, launcherScore));
            return true;
        }
        return false;
    }

    private static boolean tryBuildCarrier() throws GameActionException{
        // Too much unused resources, stop making more miners & boost score to reflect that knowledge
        // if (BuilderWrapper.adamantium >= MIN_ADAMANTIUM_STOP_CARRIERS && BuilderWrapper.mana >= MIN_ADAMANTIUM_STOP_CARRIERS){
        //     if (carrierScore < launcherScore){
        //         Comms.writeScore(RobotType.CARRIER, updateScore(RobotType.CARRIER, carrierScore + getMinCarriers()));
        //     }
        //     return false;
        // }

        // TODO: Explore resource
        if (!BuilderWrapper.hasResourcesToBuild(RobotType.CARRIER, 1))
            return false;

        int carrierReq = 1;
        if (rc.getRobotCount() > MAP_SIZE / 4 || rc.getRobotCount() > 100 || rc.getRoundNum() >= 1700) carrierReq = 3;

        if (BuilderWrapper.hasResourcesToBuild(RobotType.CARRIER, carrierReq)){
            ResourceType prioritizedResource = BuilderWrapper.getPrioritizedResource();
            if (tryConstructEnvelope(RobotType.CARRIER, BuilderWrapper.findBestSpawnLocation(RobotType.CARRIER, prioritizedResource))) {
                Comms.writeScore(RobotType.CARRIER, updateScore(RobotType.CARRIER, carrierScore));
                BuilderWrapper.setPrioritizedResource(prioritizedResource);
                rc.setIndicatorString("building carrier that prioritizes " + prioritizedResource);
                return true;
            }
        }

        return false;
    }

    private static boolean shouldBuildAmplifier() throws GameActionException{
        return launcherScore >= 10 + amplifierScore && rc.getRoundNum() >= 100;
    }

    private static boolean shouldBuildAnchor() throws GameActionException{
        return (int)((double)Comms.getRobotCount(RobotType.LAUNCHER) * MISCOUNT_FACTOR) >= getMinLaunchers();
    }

    private static boolean buildOurAnchor() throws GameActionException{
        System.out.println("Trying to build standard anchor");
        if (!rc.canBuildAnchor(Anchor.STANDARD)) return false;
        System.out.println("Built standard anchor");
        rc.buildAnchor(Anchor.STANDARD);
        anchorCountDown = (20 - rc.getRoundNum()/200) * Comms.getHeadquartersCount();
        Comms.writeScore(Anchor.STANDARD, updateScore(Anchor.STANDARD, standardAnchorScore));
        BuilderWrapper.sendAnchorCollectionCommand();
        return true;
    }

    private static boolean tryBuildStandardAnchor() throws GameActionException{
        anchorCountDown--;
        if (anchorCountDown > 0) return false;
        if (rc.getNumAnchors(Anchor.STANDARD) >= 2) return false;
        if (!BuilderWrapper.hasResourcesToBuild(Anchor.STANDARD, 1)) return false;
        if (rc.getRobotCount() > MAP_SIZE / 4) return buildOurAnchor();
        if (BuilderWrapper.hasResourcesToBuild(Anchor.STANDARD, 5)) return buildOurAnchor();
        if (rc.getRobotCount() > 70 + Math.round(rc.getRobotCount()/500) * 20) return buildOurAnchor();
        if (launcherScore < standardAnchorScore) return false;
        return buildOurAnchor();
    }

    public static boolean tryBuildAmplifier() throws GameActionException{
        if (!shouldBuildAmplifier()) return false;
        // if (amplifierScore > carrierScore) return false;
        if (amplifierScore > launcherScore) return false;
        if (!BuilderWrapper.hasResourcesToBuild(RobotType.AMPLIFIER, 1)) return false;
        if (tryConstructEnvelope(RobotType.AMPLIFIER, Comms.findNearestEnemyHeadquarterLocation())){
            Comms.writeScore(RobotType.AMPLIFIER, updateScore(RobotType.AMPLIFIER, amplifierScore));
            return true;
        }
        return false;
    }

    public static void buildUnits(boolean endangered) throws GameActionException{
        boolean builtUnit = true;
        if (endangered){
            while(rc.isActionReady() && builtUnit){
                builtUnit = false;
                if (tryBuildLauncher()) {
                    builtUnit = true;
                    continue;
                }
            }
            // The below case is specifically for when enemy HQ spawn in vision range to HQ
            if (endangered && rc.getRoundNum() < 30) 
                return;
        }
        while(rc.isActionReady() && builtUnit){
            builtUnit = false;
            if (rc.getRoundNum() == 1){
                if (tryBuildLauncher()) {
                    builtUnit = true;
                    continue;
                }
            }
            if (tryBuildStandardAnchor()) {
                builtUnit = true;
                continue;
            }
            if (tryBuildCarrier()) {
                builtUnit = true;
                continue;
            }
            if (!endangered && tryBuildAmplifier() && MAP_SIZE >= 2500) {
                builtUnit = true;
                continue;
            }
            if (tryBuildLauncher()) {
                builtUnit = true;
                continue;
            }
        }
    }
}
