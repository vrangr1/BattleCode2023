package HardlyWorkingBot;

import java.util.Map;

import battlecode.common.*;

/* 
 * Communication Integer Format:
    * 16 bits: [12 bits: location [x y] (6 bits per each coordinate)] [4 bits: SHAFlag]
*/


public class Comms extends Utils{
    
    
    ////////////////////////////////////////
    // DATA MEMBERS ////////////////////////
    ////////////////////////////////////////

    private static final int SHAFLAG_BITLENGTH = 4;
    private static final int MAX_HEADQUARTERS_CHANNLS_COUNT = 12;
    public static final int CHANNELS_COUNT_PER_HEADQUARTER = 3;
    private static final int WELLS_CHANNELS_COUNT = 10;
    private static final int ISLAND_CHANNELS_COUNT = 10;
    private static final int COMBAT_CHANNELS_COUNT = 32;
    // ununsed channels count: 0 (in the case of max count of headquarters (i.e. 4))
    private static final int TOTAL_CHANNELS_COUNT = COMMS_VARCOUNT; // 64
    private static int channelsUsed = 0;
    private static int commsHeadquarterCount = -1, commsEnemyHeadquarterCount = -1;
    private static MapLocation[] commsHeadquarterLocations = null, commsEnemyHeadquarterLocations = null;
    


    ////////////////////////////////////////
    // ENUMS: //////////////////////////////
    ////////////////////////////////////////
    
    // SHAFlags written in increasing order of priority. Unsure of NOT_A_LOCATION_MESSAGE placement in the priority order.
    public static enum SHAFlag { // reserving 4 bits for this
        EMPTY_MESSAGE,                      // 0x0
        
        // HEADQUARTERS' MESSAGES' TYPES
        HEADQUARTER_LOCATION,               // 0x1
        ENEMY_HEADQUARTER_LOCATION,         // 0x2
        COLLECT_ANCHOR,                     // 0x3

        // WELLS CHANNELS MESSAGES' TYPES
        WELL_LOCATION,                      // 0x4
        // ADAMANTIUM_WELL_LOCATION,           // 0x5
        // MANA_WELL_LOCATION,                 // 0x6
        // ELIXIR_WELL_LOCATION,               // 0x7

        // ISLAND CHANNELS MESSAGES' TYPES
        OCCUPIED_ISLAND,                    // 0x8
        UNOCCUPIED_ISLAND,                  // 0x9

        // COMBAT CHANNELS MESSAGES' TYPES
        COMBAT_LOCATION,                    // 0xA
        ISLAND_DEFENSE_NEEDED,              // 0xB

        // COMMS UTILITY TYPE
        ARRAY_HEAD;                         // 0xC
        // Can go upto 0xF. If more needed, let me know. I can make do without a few types.

        public boolean higherPriority(SHAFlag flag){
            return (this.ordinal() > flag.ordinal());
        }

        public boolean lesserPriority(SHAFlag flag){
            return (this.ordinal() < flag.ordinal());
        }

        public boolean lesserOrEqualPriority(SHAFlag flag){
            return (this.ordinal() <= flag.ordinal());
        }
    }

    public static SHAFlag[] SHAFlags = SHAFlag.values();

    public static enum COMM_TYPE{
        WELLS,          // 0x1
        COMBAT,         // 0x2
        ISLAND,         // 0x3
        HEADQUARTER;    // 0x4

        public int channelStart, channelStop, channelHead, channelHeadArrayLocationIndex;
    }



    ////////////////////////////////////////
    // GENERAL UTILITY METHODS /////////////
    ////////////////////////////////////////

    public static int getNumChannelsUsed(){
        return channelsUsed;
    }



    ////////////////////////////////////////
    // PRIVATE UTILITY METHODS /////////////
    ////////////////////////////////////////

    private static int incrementHead(int i, COMM_TYPE type){
        return Math.max(type.channelStart, (i + 1) % type.channelStop);
    }

    private static int decrementHead(int i, COMM_TYPE type){
        if (i - 1 < type.channelStart) return type.channelStop - 1;
        return i - 1;
    }

    private static void updateHead(COMM_TYPE type, int newHead) throws GameActionException{
        writeSHAFlagMessage(newHead, SHAFlag.ARRAY_HEAD, type.channelHeadArrayLocationIndex);
    }

    /**
     * Gets the first empty channel it can find. If none found, gets the last used channel.
     * @param type Communication type: WELLS, COMBAT only
     * @returns channel index
     * @throws GameActionException
     */
    private static int getWriteChannel(COMM_TYPE type) throws GameActionException{
        type.channelHead = readMessageWithoutSHAFlag(type.channelHeadArrayLocationIndex);
        int i = type.channelHead;
        do{
            if (readSHAFlagType(i) == SHAFlag.EMPTY_MESSAGE)
                return i;
            i = incrementHead(i, type);
        }while(i != type.channelHead);
        return i;
    }

    /**
     * Gets the first channel of lesser or equal priority it can find. If none found, gets the oldest channel used irrespective of priority.
     * @param type Communication type: WELLS, COMBAT only
     * @param flag Message type: EMPTY_MESSAGE, HEADQUARTER_LOCATION...
     * @returns channel index
     * @throws GameActionException
     */
    private static int getWriteChannelOfLesserPriority(COMM_TYPE type, SHAFlag flag) throws GameActionException{
        type.channelHead = readMessageWithoutSHAFlag(type.channelHeadArrayLocationIndex);
        int i = type.channelHead;
        do{
            if (readSHAFlagFromMessage(rc.readSharedArray(i)).lesserOrEqualPriority(flag))
                return i;
            i = incrementHead(i, type);
        }while(i != type.channelHead);
        return i;
    }

    /**
     * Gets the first channel of <strong>STRICTLY</strong> lesser priority it can find. If none found, returns -1.
     * @param type Communication type: WELLS, COMBAT only
     * @param flag Message type: EMPTY_MESSAGE, HEADQUARTER_LOCATION...
     * @returns channel index or -1
     * @throws GameActionException
     */
    private static int getWriteChannelOfStrictlyLesserPriority(COMM_TYPE type, SHAFlag flag) throws GameActionException{
        type.channelHead = readMessageWithoutSHAFlag(type.channelHeadArrayLocationIndex);
        int i = type.channelHead;
        do{
            if (readSHAFlagFromMessage(rc.readSharedArray(i)).lesserOrEqualPriority(flag))
                return i;
            i = incrementHead(i, type);
        }while(i != type.channelHead);
        return -1;
    }



    ////////////////////////////////////////
    // INITIALIZATION STUFF ////////////////
    ////////////////////////////////////////
    
    private static void allocateChannels(COMM_TYPE type, int channelCount) throws GameActionException{
        if (channelCount + channelsUsed > TOTAL_CHANNELS_COUNT)
            throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Too many channels being used");
        type.channelStart = channelsUsed;
        type.channelStop = channelsUsed + channelCount - 1;
        channelsUsed += channelCount;
        type.channelHead = type.channelStart;
        type.channelHeadArrayLocationIndex = type.channelStop;
        if (rc.canWriteSharedArray(type.channelHeadArrayLocationIndex, type.channelHead))
            writeSHAFlagMessage(type.channelHead, SHAFlag.ARRAY_HEAD, type.channelHeadArrayLocationIndex);
    }

    public static void writeHeadquarterLocation(MapLocation headquarterLoc) throws GameActionException{
        if (rc.getRoundNum() != 1)
            throw new GameActionException(GameActionExceptionType.ROUND_OUT_OF_RANGE, "round number has to be 1 here.");
        for (int i = 0; i < MAX_HEADQUARTERS_CHANNLS_COUNT; i += CHANNELS_COUNT_PER_HEADQUARTER){
            SHAFlag flag = readSHAFlagType(i);
            if (flag == SHAFlag.EMPTY_MESSAGE){
                writeSHAFlagMessage(headquarterLoc, SHAFlag.HEADQUARTER_LOCATION, i);
                commsHeadquarterCount = i/CHANNELS_COUNT_PER_HEADQUARTER;
                BotHeadquarters.headquarterIndex = i + 2;
                break;
            }
            else if (flag == SHAFlag.HEADQUARTER_LOCATION){
                commsHeadquarterCount = i/CHANNELS_COUNT_PER_HEADQUARTER;
                continue;
            }
            assert false;
        }
    }

    private static void headquarterChannelsInit() throws GameActionException{
        if (rc.getType() == RobotType.HEADQUARTERS && rc.getRoundNum() == 1){
            writeHeadquarterLocation(rc.getLocation());
            return;
        }
        int headquarterCount = getHeadquartersCount();
        channelsUsed += headquarterCount * 3;
    }

    public static void initCommunicationsArray() throws GameActionException{
        channelsUsed = 0;
        commsHeadquarterCount = -1;
        commsEnemyHeadquarterCount = 0;
        commsHeadquarterLocations = null;
        commsEnemyHeadquarterLocations = null;
        headquarterChannelsInit();
        if (rc.getRoundNum() == 1){
            if (rc.getType() != RobotType.HEADQUARTERS)
                throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "unexpected behavior");
            return;
        }
        allocateChannels(COMM_TYPE.WELLS, WELLS_CHANNELS_COUNT);
        allocateChannels(COMM_TYPE.ISLAND, ISLAND_CHANNELS_COUNT);
        allocateChannels(COMM_TYPE.COMBAT, COMBAT_CHANNELS_COUNT);
    }



    ////////////////////////////////////////
    // READ METHODS ////////////////////////
    ////////////////////////////////////////

    public static SHAFlag readSHAFlagFromMessage(int message){
        return SHAFlags[message & 0xF];
    }

    public static SHAFlag readSHAFlagType(int channelIndex) throws GameActionException{
        int message = rc.readSharedArray(channelIndex);
        return readSHAFlagFromMessage(message);
    }

    public static MapLocation readLocationFromMessage(int message){
        return mapLocationFromInt((message >> 4));
    }

    public static int readMessageWithoutSHAFlag(int channel) throws GameActionException{
        return (rc.readSharedArray(channel) >> 4);
    }



    ////////////////////////////////////////
    // WRITE METHODS ///////////////////////
    ////////////////////////////////////////

    /**
     * Writes the coordinates of the given MapLocation to the given channel.
     * @param loc : MapLocation that is to be hashed and written into the given channel
     * @param flag : SHAFlag (i.e. message type) of the given message
     * @param channel : channel index into which the message is to be written
     * @throws GameActionException
     * @BytecodeCost At least 75
     */
    public static void writeSHAFlagMessage(MapLocation loc, SHAFlag flag, int channel) throws GameActionException{
        int value = intFromMapLocation(loc);
        rc.writeSharedArray(channel, (value << SHAFLAG_BITLENGTH) | flag.ordinal());
    }

    /**
     * Writes the given integer message into the given channel. Mostly for non location type messages
     * @param message : integer message that is to be written into the given channel. Must be < 4095 and >= 0
     * @param flag : SHAFlag (i.e. message type) of the given message
     * @param channel : index into which the message is to be written
     * @throws GameActionException
     * @BytecodeCost : ~75
     */
    public static void writeSHAFlagMessage(int message, SHAFlag flag, int channel) throws GameActionException{
        rc.writeSharedArray(channel, (message << SHAFLAG_BITLENGTH) | flag.ordinal());
    }

    public static void writeEnemyHeadquarterLocation(MapLocation loc) throws GameActionException{
        boolean empty = false;
        if (rc.getRoundNum() == 1) assert false;
        int headquarterCount = getHeadquartersCount();
        if (commsEnemyHeadquarterCount == headquarterCount)
            return;
        if (commsEnemyHeadquarterLocations == null)
            commsEnemyHeadquarterLocations = createNullMapLocations(headquarterCount);

        for (int i = COMM_TYPE.HEADQUARTER.channelStart + 1; i < COMM_TYPE.HEADQUARTER.channelStop; i += CHANNELS_COUNT_PER_HEADQUARTER){
            int message = rc.readSharedArray(i);
            SHAFlag flag = readSHAFlagFromMessage(message);
            if (flag == SHAFlag.ENEMY_HEADQUARTER_LOCATION && loc == readLocationFromMessage(message)){
                commsEnemyHeadquarterCount = Math.max(i/CHANNELS_COUNT_PER_HEADQUARTER, commsEnemyHeadquarterCount);
                if (commsEnemyHeadquarterLocations[i/CHANNELS_COUNT_PER_HEADQUARTER].x == -1)
                    commsEnemyHeadquarterLocations[i/CHANNELS_COUNT_PER_HEADQUARTER] = loc;
                return;
            }
            else if (flag == SHAFlag.ENEMY_HEADQUARTER_LOCATION && empty)
                throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "shouldn't have happened");
            else if (flag == SHAFlag.ENEMY_HEADQUARTER_LOCATION){
                if (commsEnemyHeadquarterLocations[i/CHANNELS_COUNT_PER_HEADQUARTER].x == -1)
                    commsEnemyHeadquarterLocations[i/CHANNELS_COUNT_PER_HEADQUARTER] = readLocationFromMessage(message);
                commsEnemyHeadquarterCount = Math.max(i/CHANNELS_COUNT_PER_HEADQUARTER, commsEnemyHeadquarterCount);
                if (commsEnemyHeadquarterCount > getHeadquartersCount())
                    throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "enemy hq count. How???");
                continue;
            }
            else if (flag == SHAFlag.EMPTY_MESSAGE && !empty){
                empty = true;
                if ((i/CHANNELS_COUNT_PER_HEADQUARTER) <= commsEnemyHeadquarterCount)
                    throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "enemy hq count. How2222???");
                commsEnemyHeadquarterCount = i/CHANNELS_COUNT_PER_HEADQUARTER;
                if (commsEnemyHeadquarterCount > getHeadquartersCount())
                    throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "enemy hq count. How???");
                writeSHAFlagMessage(loc, SHAFlag.ENEMY_HEADQUARTER_LOCATION, i);
                commsEnemyHeadquarterLocations[i/CHANNELS_COUNT_PER_HEADQUARTER] = loc;
                return;
            }
            else assert false : "logical error in writeEnemyHeadquarterLocation func";
        }
    }

    /** Writes to the communication channels (WELLS, ISLAND, OR COMBAT). This searches for an empty channel in which it can write the message. If it can't find any empty channel, it'll write to the oldest accessed shared channel.
     * @param type : One of Comms.COMM_TYPE.LEAD or Comms.COMM_TYPE.ISLAND or Comms.COMM_TYPE.COMBAT depending on whose channels need to be written into.
     * @param message : the int message that you want to write to the channel. In most cases it's going to be int form of a MapLocation. There's another overloaded function with the same name that accepts MapLocation as input.
     * @param flag : the SHAFlag flag that denotes the type of the message: COMBAT_LOCATION, ADAMANTIUM_WELL_LOCATION, etc.
     * @return the channel index to which the message was written
     * @BytecodeCost<pre>
     *      When COMM_TYPE is WELLS   : ~100 at max
     *When COMM_TYPE is ISLAND : ~100 at max
     *When COMM_TYPE is COMBAT : ~200 at max
     * </pre>
     * @throws GameActionException
     * **/
    public static int writeMessage(COMM_TYPE type, int message, SHAFlag flag) throws GameActionException{
        int channel = getWriteChannel(type);
        writeSHAFlagMessage(message, flag, channel);
        updateHead(type, incrementHead(channel, type));
        return channel;
    }

    /** Writes to the communication channels (WELLS, ISLAND, OR COMBAT). This searches for an empty channel in which it can write the message. If it can't find any empty channel, it'll write to the oldest accessed shared channel.
     * @param type : One of Comms.COMM_TYPE.LEAD or Comms.COMM_TYPE.ISLAND or Comms.COMM_TYPE.COMBAT depending on whose channels need to be written into.
     * @param loc : the location that you want to write into a communication channel. If you want to send an int message there's another overloaded function with the same name that accepts int as input.
     * @param flag : the SHAFlag flag that denotes the type of the message: COMBAT_LOCATION, ADAMANTIUM_WELL_LOCATION, etc.
     * @return the channel index to which the message was written
     * @BytecodeCost<pre>
     *      When commType is LEAD   : ~100 at max
     *When commType is ISLAND : ~200 at max
     *When commType is COMBAT : ~200 at max
     * </pre>
     * **/
    public static int writeMessage(COMM_TYPE type, MapLocation loc, SHAFlag flag) throws GameActionException{
        int message = intFromMapLocation(loc);
        return writeMessage(type, message, flag);
    }

    /** Writes to the communication channel. This directly searches for a channel whose information holds less or <strong>EQUAL</strong> priority than the current message (priority decided by SHAFlag).
     * @param type : One of Comms.COMM_TYPE.LEAD or Comms.COMM_TYPE.ISLAND or Comms.COMM_TYPE.COMBAT depending on whose channels need to be written into.
     * @param message : the int message that you want to write to the channel. In most cases it's going to be int form of a MapLocation. There's another overloaded function with the same name that accepts MapLocation as input.
     * @param flag : the SHAFlag flag that denotes the type of the message: COMBAT_LOCATION, ADAMANTIUM_WELL_LOCATION, etc.
     * @return the channel index to which the message was written
     * @BytecodeCost<pre>
     *      When commType is LEAD   : ~100 at max
     *When commType is ISLAND : ~200 at max
     *When commType is COMBAT : ~200 at max
     * </pre>
     * **/
    public static int writeAndOverwriteLesserPriorityMessage(COMM_TYPE type, int message, SHAFlag flag) throws GameActionException{
        int channel = getWriteChannelOfLesserPriority(type, flag);
        writeSHAFlagMessage(message, flag, channel);
        updateHead(type, incrementHead(channel, type));
        return channel;
    }


    /** Writes to the communication channel. This directly searches for a channel whose information holds less or <strong>EQUAL</strong> priority than the current message (priority decided by SHAFlag).
     * @param type : One of Comms.COMM_TYPE.LEAD or Comms.COMM_TYPE.ISLAND or Comms.COMM_TYPE.COMBAT depending on whose channels need to be written into.
     * @param loc : the location that you want to write into a communication channel. If you want to send an int message there's another overloaded function with the same name that accepts int as input.
     * @param flag : the SHAFlag flag that denotes the type of the message: COMBAT_LOCATION, ADAMANTIUM_WELL_LOCATION, etc.
     * @return the channel index to which the message was written
     * @BytecodeCost<pre>
     *      When commType is LEAD   : ~100 at max
     *When commType is ISLAND : ~200 at max
     *When commType is COMBAT : ~200 at max
     * </pre>
     * **/
    public static int writeAndOverwriteLesserPriorityMessage(COMM_TYPE type, MapLocation loc, SHAFlag flag) throws GameActionException{
        int channel = getWriteChannelOfLesserPriority(type, flag);
        writeSHAFlagMessage(loc, flag, channel);
        updateHead(type, incrementHead(channel, type));
        return channel;
    }

    /** Writes to the communication channel. This directly searches for a channel whose information holds <strong>STRICTLY</strong> less priority than the current message (priority decided by SHAFlag). If none are found, returns -1.
     * @param type : One of Comms.COMM_TYPE.LEAD or Comms.COMM_TYPE.ISLAND or Comms.COMM_TYPE.COMBAT depending on whose channels need to be written into.
     * @param message : the int message that you want to write to the channel. In most cases it's going to be int form of a MapLocation. There's another overloaded function with the same name that accepts MapLocation as input.
     * @param flag : the SHAFlag flag that denotes the type of the message: COMBAT_LOCATION, ADAMANTIUM_WELL_LOCATION, etc.
     * @return the channel index to which the message was written. If no channels of strictly lesser priority can be found. Returns -1.
     * @BytecodeCost<pre>
     *      When commType is LEAD   : ~100 at max
     *When commType is ISLAND : ~200 at max
     *When commType is COMBAT : ~200 at max
     * </pre>
     * **/
    public static int writeAndOverwriteStrictlyLesserPriorityMessage(COMM_TYPE type, int message, SHAFlag flag) throws GameActionException{
        int channel = getWriteChannelOfStrictlyLesserPriority(type, flag);
        if (channel != -1){
            writeSHAFlagMessage(message, flag, channel);
            updateHead(type, incrementHead(channel, type));
        }
        return channel;
    }

    /** Writes to the communication channel. This directly searches for a channel whose information holds <strong>STRICTLY</strong> less priority than the current message (priority decided by SHAFlag). If none are found, returns -1.
     * @param type : One of Comms.COMM_TYPE.LEAD or Comms.COMM_TYPE.ISLAND or Comms.COMM_TYPE.COMBAT depending on whose channels need to be written into.
     * @param loc : the location that you want to write into a communication channel. If you want to send an int message there's another overloaded function with the same name that accepts int as input.
     * @param flag : the SHAFlag flag that denotes the type of the message: COMBAT_LOCATION, ADAMANTIUM_WELL_LOCATION, etc.
     * @return the channel index to which the message was written. If no channels of strictly lesser priority can be found. Returns -1.
     * @BytecodeCost<pre>
     *      When commType is LEAD   : ~100 at max
     *When commType is ISLAND : ~200 at max
     *When commType is COMBAT : ~200 at max
     * </pre>
     * **/
    public static int writeAndOverwriteStrictlyLesserPriorityMessage(COMM_TYPE type, MapLocation loc, SHAFlag flag) throws GameActionException{
        int channel = getWriteChannelOfStrictlyLesserPriority(type, flag);
        if (channel != -1){
            writeSHAFlagMessage(loc, flag, channel);
            updateHead(type, incrementHead(channel, type));
        }
        return channel;
    }



    ////////////////////////////////////////
    // WIPE CHANNELS METHODS ///////////////
    ////////////////////////////////////////

    /** Writes 0 to the given channel.
     * @param channel : the input channel to be set to 0.
     * @BytecodeCost 75
     */
    public static void wipeChannel(int channel) throws GameActionException{
        rc.writeSharedArray(channel, 0);
    }

    /** Writes 0 to all channels. Heavy bytecode cost
     * @BytecodeCost 6400
     */
    public static void wipeAllChannel() throws GameActionException{
        for (int i = 0; i < TOTAL_CHANNELS_COUNT; ++i)
            wipeChannel(i);
    }

    /**
     * Wipe channels of a particular type
     * @param type COMM_TYPE: WELLS, ISLAND, OR COMBAT
     * @throws GameActionException
     */
    public static void wipeChannelsCOMMTYPE(COMM_TYPE type) throws GameActionException{
        for (int i = type.channelStart; i < type.channelStop; ++i)
            wipeChannel(i);
        type.channelHead = type.channelStart;
        writeSHAFlagMessage(type.channelHead, SHAFlag.ARRAY_HEAD, type.channelHeadArrayLocationIndex);
    }


    public static void wipeThisLocationFromChannels(COMM_TYPE type, SHAFlag flag, MapLocation loc) throws GameActionException{
        if (!rc.canWriteSharedArray(0, 0)) return;
        for (int i = type.channelStart; i < type.channelStop; ++i){
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) == flag && readLocationFromMessage(message) == loc){
                rc.writeSharedArray(i, 0);
                continue;
            }
        }
    }



    ////////////////////////////////////////
    // FIND AND GET METHODS ////////////////
    ////////////////////////////////////////

    /**
     * Gets the count of headquarters from the shared array
     * @BytecodeCost ~10.
     */
    public static int getHeadquartersCount() throws GameActionException{
        if (commsHeadquarterCount != -1 && rc.getRoundNum() > 1) return commsHeadquarterCount;
        MapLocation[] tempLocs = new MapLocation[GameConstants.MAX_STARTING_HEADQUARTERS];
        int count = 0;
        for (int i = 0; i < MAX_HEADQUARTERS_CHANNLS_COUNT; i += CHANNELS_COUNT_PER_HEADQUARTER){
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) == SHAFlag.HEADQUARTER_LOCATION){
                count++;
                if (commsHeadquarterLocations == null) 
                    tempLocs[i/CHANNELS_COUNT_PER_HEADQUARTER] = readLocationFromMessage(message);
            }
            else break;
        }
        if ((count <= 0 && rc.getRoundNum() > 1) || count > 4)
            throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "headquarter count can't be this. count: " + Integer.toString(count));
        commsHeadquarterCount = count;
        COMM_TYPE.HEADQUARTER.channelStart = 0;
        COMM_TYPE.HEADQUARTER.channelStop = count * CHANNELS_COUNT_PER_HEADQUARTER - 1;
        if (commsHeadquarterLocations == null){
            commsHeadquarterLocations = new MapLocation[commsHeadquarterCount];
            switch(count){
                case 4: commsHeadquarterLocations[3] = tempLocs[3];
                case 3: commsHeadquarterLocations[2] = tempLocs[2];
                case 2: commsHeadquarterLocations[1] = tempLocs[1];
                case 1: commsHeadquarterLocations[0] = tempLocs[0]; break;
                default: assert false;
            }
        }
        return count;
    }

    public static MapLocation[] getAlliedHeadquartersLocationsList() throws GameActionException{
        if (rc.getRoundNum() == 1) return null;
        if (commsHeadquarterLocations != null) return commsHeadquarterLocations;
        int headquarterCount = getHeadquartersCount();
        MapLocation[] commsHeadquarterLocations = new MapLocation[headquarterCount];
        for (int i = COMM_TYPE.HEADQUARTER.channelStart; i < COMM_TYPE.HEADQUARTER.channelStop; i += CHANNELS_COUNT_PER_HEADQUARTER){
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) != SHAFlag.HEADQUARTER_LOCATION)
                throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "getting hq loc: how did this happen");
            commsHeadquarterLocations[i/CHANNELS_COUNT_PER_HEADQUARTER] = readLocationFromMessage(message);
        }
        return commsHeadquarterLocations;
    }

    public static MapLocation findNearestHeadquarter() throws GameActionException{
        if (rc.getRoundNum() == 1) assert false;
        MapLocation[] headquarters = getAlliedHeadquartersLocationsList();
        MapLocation optLoc = headquarters[0], myLoc = rc.getLocation();
        int optDist = myLoc.distanceSquaredTo(optLoc), headquarterCount = getHeadquartersCount(), dist;
        for (int i = 1; i < headquarterCount; ++i){
            dist = myLoc.distanceSquaredTo(headquarters[i]);
            if (dist < optDist){
                optLoc = headquarters[i];
                optDist = dist;
            }
        }
        return optLoc;
    }

    public static MapLocation[] getEnemyHeadquartersLocationsList() throws GameActionException{
        int headquarterCount = getHeadquartersCount();
        if (commsEnemyHeadquarterCount > headquarterCount)
            throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "enemy hq count moar. how?");
        if (commsEnemyHeadquarterCount == headquarterCount){
            if (commsEnemyHeadquarterLocations == null) assert false;
            return commsEnemyHeadquarterLocations;
        }
        if (commsEnemyHeadquarterCount == 0){
            if (commsEnemyHeadquarterLocations != null) assert false;
            commsEnemyHeadquarterLocations = createNullMapLocations(headquarterCount);
        }
        for (int i = COMM_TYPE.HEADQUARTER.channelStart + 1; i < COMM_TYPE.HEADQUARTER.channelStop; i += CHANNELS_COUNT_PER_HEADQUARTER){
            if (commsEnemyHeadquarterLocations[i/CHANNELS_COUNT_PER_HEADQUARTER].x != -1) continue;
            int message = rc.readSharedArray(i);
            SHAFlag flag = readSHAFlagFromMessage(message);
            if (flag == SHAFlag.EMPTY_MESSAGE) 
                return commsEnemyHeadquarterLocations;
            if (flag != SHAFlag.ENEMY_HEADQUARTER_LOCATION)
                throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "not enemy hq location. No!");
            commsEnemyHeadquarterLocations[i/CHANNELS_COUNT_PER_HEADQUARTER] = readLocationFromMessage(message);
            commsEnemyHeadquarterCount = i/CHANNELS_COUNT_PER_HEADQUARTER;
        }
        return commsEnemyHeadquarterLocations;
    }

    /**
     * Finds and returns the location from the first message found in the communication channels that has the given SHAFlag.
     * @param type : Search for the message in this type's channels;
     * @param flag : the SHAFlag that is being searched for in the comms channels.
     * @return the first MapLocation found of the correct SHAFlag type or null if none found.
     * @throws GameActionException
     */
    public static MapLocation findLocationOfThisType(COMM_TYPE type, SHAFlag flag) throws GameActionException{
        type.channelHead = readMessageWithoutSHAFlag(type.channelHeadArrayLocationIndex);
        int i = type.channelHead;
        do{
            i = decrementHead(i, type);
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) == flag){
                return readLocationFromMessage(message);
            }
        }while(i != type.channelHead);
        assert false;
        return null;
    }

    public static MapLocation findLocationOfThisTypeAndWipeChannel(COMM_TYPE type, SHAFlag flag) throws GameActionException{
        int channel = -1;
        MapLocation loc = null;
        type.channelHead = readMessageWithoutSHAFlag(type.channelHeadArrayLocationIndex);
        int i = type.channelHead;
        do{
            i = decrementHead(i, type);
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) == flag){
                channel = i;
                loc = readLocationFromMessage(message);
                break;
            }
        }while(i != type.channelHead);
        
        if (channel != -1) wipeChannel(channel);
        return loc;
    }

    /**
     * Finds and returns the nearest location (to the reference location) that has the given SHAFlag from the communication channels.
     * @param loc  : The reference location.
     * @param type : Search for the message in this type's channels (WELLS or ISLAND or COMBAT);
     * @param flag : the SHAFlag that is being searched for in the comms channels.
     * @return the nearest MapLocation found of the correct SHAFlag type or null if none found.
     * @throws GameActionException
     */
    public static MapLocation findNearestLocationOfThisType(MapLocation loc, COMM_TYPE type, SHAFlag flag) throws GameActionException{
        MapLocation nearestLoc = null;
        int nearestDist = -1, newDist;
        for (int i = type.channelStart; i < type.channelStop; ++i){
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) != flag) continue;
            MapLocation newLoc = readLocationFromMessage(message);
            newDist = loc.distanceSquaredTo(newLoc);
            if (nearestLoc == null || nearestDist > newDist){
                nearestLoc = newLoc;
                nearestDist = newDist;
            }
        }
        return nearestLoc;
    }

    public static MapLocation findNearestLocationOfThisTypeOutOfVision(MapLocation loc, COMM_TYPE type, SHAFlag flag) throws GameActionException{
        MapLocation nearestLoc = null;
        for (int i = type.channelStart; i < type.channelStop; ++i){
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) != flag) continue;
            MapLocation newLoc = readLocationFromMessage(message);
            // We don't want locations in vision
            if (!rc.canSenseLocation(newLoc) && (nearestLoc == null || loc.distanceSquaredTo(nearestLoc) > loc.distanceSquaredTo(newLoc))) 
                nearestLoc = newLoc;
        }
        return nearestLoc;
    }


    public static MapLocation findNearestLocationOfThisTypeOutOfVisionAndWipeChannel(MapLocation loc, COMM_TYPE type, SHAFlag flag) throws GameActionException{
        MapLocation nearestLoc = null;
        int channel = -1;
        for (int i = type.channelStart; i < type.channelStop; ++i){
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) != flag) continue;
            MapLocation newLoc = readLocationFromMessage(message);
            // We don't want locations in vision
            if (!rc.canSenseLocation(newLoc) && (nearestLoc == null || loc.distanceSquaredTo(nearestLoc) > loc.distanceSquaredTo(newLoc))){
                nearestLoc = newLoc;
                channel = i;
            }
        }
        if (channel != -1) wipeChannel(channel);
        return nearestLoc;
    }

    /**
     * Finds and returns the nearest location (to the reference location) that has the given SHAFlag from the communication channels and wipes the channel after reading.
     * @param loc  : The reference location.
     * @param type : Search for the message in this type's channels (WELLS or ISLAND or COMBAT);
     * @param flag : the SHAFlag that is being searched for in the comms channels.
     * @return the nearest MapLocation found of the correct SHAFlag type or null if none found.
     * @throws GameActionException
     */
    public static MapLocation findNearestLocationOfThisTypeAndWipeChannel(MapLocation loc, COMM_TYPE type, SHAFlag flag) throws GameActionException{
        MapLocation nearestLoc = null;
        int channel = -1;
        for (int i = type.channelStart; i < type.channelStop; ++i){
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) != flag) continue;
            MapLocation newLoc = readLocationFromMessage(message);
            if (nearestLoc == null || loc.distanceSquaredTo(nearestLoc) > loc.distanceSquaredTo(newLoc)){ 
                nearestLoc = newLoc;
                channel = i;
            }
        }
        if (channel != -1)
            wipeChannel(channel);
        return nearestLoc;
    }

    public static MapLocation checkIfAnchorProduced() throws GameActionException{
        for (int i = 0; i < MAX_HEADQUARTERS_CHANNLS_COUNT; i += CHANNELS_COUNT_PER_HEADQUARTER){
            int message = rc.readSharedArray(i + 2);
            if (readSHAFlagFromMessage(message) == SHAFlag.COLLECT_ANCHOR){
                assert rc.getType() == RobotType.CARRIER;
                BotCarrier.collectAnchorHQidx = i + 2;
                return readLocationFromMessage(message);
            }
        }
        return null;
    }
}