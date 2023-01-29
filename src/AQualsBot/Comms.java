package AQualsBot;

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
    private static final int CARRIER_SCORE_CHANNEL = 0;
    private static final int LAUNCHER_SCORE_CHANNEL = 1;
    private static final int AMPLIFIER_SCORE_CHANNEL = 2;
    private static final int BOOSTER_SCORE_CHANNEL = 3;
    // private static final int DESTABILIZER_SCORE_CHANNEL = 4;
    private static final int ELIXIR_WELL_LOCATION_CHANNEL = 4;
    private static final int STANDARD_ANCHOR_SCORE_CHANNEL = 5;
    private static final int LAUNCHER_COUNT_CHANNEL = 6;
    private static final int AMPLIFIER_COUNT_CHANNEL = 7;
    public static final int SYMMETRY_CHANNEL = 8;
    // private static final int ACCELERATING_ANCHOR_COUNT_CHANNEL = 6;
    public static final int START_CHANNEL_BANDS = 9;
    private static final int MAX_HEADQUARTERS_CHANNELS_COUNT = 12;
    public static final int CHANNELS_COUNT_PER_HEADQUARTER = 3;
    private static final int WELLS_CHANNELS_COUNT = 13;
    private static final int OUR_ISLANDS_CHANNELS_COUNT = 4;
    private static final int NEUTRAL_ISLAND_CHANNELS_COUNT = 10;
    private static final int AMPLIFIER_CHANNELS_COUNT = 0;
    private static final int COMBAT_CHANNELS_COUNT = 16;

    // ununsed channels count: 0 (in the case of max count of headquarters (i.e. 4))
    private static final int TOTAL_CHANNELS_COUNT = COMMS_VARCOUNT; // 64
    private static int channelsUsed = 0;
    private static int commsHeadquarterCount = -1, commsEnemyHeadquarterCount = -1;
    private static MapLocation[] commsHeadquarterLocations = null, commsEnemyHeadquarterLocations = null;

    // COLLECT_ANCHOR CHANNEL STUFF (changing the following will NOT correspondingly change the functions. Hardcoded values there....):
    private static final int SHAFlagBits = 1; // don't change
    public static int resourcePrioritizationBits = 10;
    private static int anchorCountBits = 5;                // TOTAL: 16

    // kth Nearest Headquarter
    private static MapLocation[] sortedHeadquarters = null;

    // Saving unwritten locations stuff
    private static MapLocation[] locationsToWrite = null;
    private static SHAFlag[] locationsToWriteFlags = null;
    private static int savedLocationsCount = 0;
    private static final int MAX_LOCATIONS_TO_SAVE = 20;
    private static final boolean OVERWRITE_OLDER_SAVED_LOCATIONS = true;
    private static boolean wrapAround = false;

    // Wiping or saving locations stuff
    private static MapLocation[] locationsToWipe = null;
    private static SHAFlag[] locationsToWipeFlags = null;
    private static int locationsToWipeCount = 0;
    private static final int MAX_LOCATIONS_TO_WIPE = 10;
    private static final boolean OVERWRITE_OLDER_WIPED_LOCATIONS = true;
    private static boolean wipeWrapAround = false;
    private static boolean saveElixirWellMade = false;

    


    ////////////////////////////////////////
    // ENUMS: //////////////////////////////
    ////////////////////////////////////////
    
    // SHAFlags written in increasing order of priority. Unsure of NOT_A_LOCATION_MESSAGE placement in the priority order.
    public static enum SHAFlag { // reserving 4 bits for this
        EMPTY_MESSAGE,                      // 0x0
        
        // HEADQUARTERS' MESSAGES' TYPES
        COLLECT_ANCHOR,                     // 0x1 // Don't change the ordinal of this. It is used in the code.
        HEADQUARTER_LOCATION,               // 0x2
        ENEMY_HEADQUARTER_LOCATION,         // 0x3

        // WELLS CHANNELS MESSAGES' TYPES
        // WELL_LOCATION,                      // 0x4
        ADAMANTIUM_WELL_LOCATION,           // 0x4
        MANA_WELL_LOCATION,                 // 0x5
        ELIXIR_WELL_LOCATION,               // 0x6

        // OUR_ISLANDS CHANNELS MESSAGES' TYPES
        OUR_ISLAND,                         // 0x7

        // ISLAND CHANNELS MESSAGES' TYPES
        // OCCUPIED_ISLAND,                    // 0x8
        UNOCCUPIED_ISLAND,                  // 0x8

        // COMBAT CHANNELS MESSAGES' TYPES
        COMBAT_LOCATION,                    // 0x9
        CLOUD_COMBAT_LOCATION,              // 0xA

        // AMPLIFIER CHANNELS MESSAGES' TYPES
        AMPLIFIER_LOCATION,                 // 0xB
        ANCHOR_DEFENSE_NEEDED,              // 0xC

        // COMMS UTILITY TYPE
        ARRAY_HEAD;                         // 0xD
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

    private static SHAFlag[] SHAFlags = SHAFlag.values();

    public static enum COMM_TYPE{
        WELLS,           // 0x0
        COMBAT,          // 0x1
        OUR_ISLANDS,     // 0x2
        NEUTRAL_ISLANDS, // 0x3
        HEADQUARTER,     // 0x4
        AMPLIFIER;       // 0x5

        public int channelStart, channelStop, channelHead, channelHeadArrayLocationIndex;
    }

    public static enum RESOURCE_PRIORITIZATION{
        NOTHING,        // 0x0
        ADAMANTIUM,     // 0x1
        MANA,           // 0x2
        ELIXIR;         // 0x3
    }

    private static RESOURCE_PRIORITIZATION[] resourcePrioritizations = RESOURCE_PRIORITIZATION.values();



    ////////////////////////////////////////
    // GENERAL UTILITY METHODS /////////////
    ////////////////////////////////////////

    public static int getNumChannelsUsed(){
        return channelsUsed;
    }

    public static SHAFlag resourceFlag(ResourceType resourceType){
        switch(resourceType){
            case ADAMANTIUM:
                return SHAFlag.ADAMANTIUM_WELL_LOCATION;
            case MANA:
                return SHAFlag.MANA_WELL_LOCATION;
            case ELIXIR:
                return SHAFlag.ELIXIR_WELL_LOCATION;
            default: assert false;
        }
        assert false;
        return null;
    }

    public static void printCOMM_TYPEDetails(COMM_TYPE type){
        System.out.println("COMM_TYPE: " + type);
        System.out.println("channelStart: " + type.channelStart);
        System.out.println("channelStop: " + type.channelStop);
        // System.out.println("channelHead: " + type.channelHead);
        // System.out.println("channelHeadArrayLocationIndex: " + type.channelHeadArrayLocationIndex);
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

    private static COMM_TYPE getCOMM_TYPEFromSHAFlag(SHAFlag flag){
        switch(flag){
            case EMPTY_MESSAGE: assert false; break;
            case ADAMANTIUM_WELL_LOCATION: return COMM_TYPE.WELLS;
            case MANA_WELL_LOCATION: return COMM_TYPE.WELLS;
            case ELIXIR_WELL_LOCATION : return COMM_TYPE.WELLS;
            case HEADQUARTER_LOCATION : return COMM_TYPE.HEADQUARTER;
            case ENEMY_HEADQUARTER_LOCATION : return COMM_TYPE.HEADQUARTER;
            case COLLECT_ANCHOR : return COMM_TYPE.HEADQUARTER;
            case AMPLIFIER_LOCATION : return COMM_TYPE.AMPLIFIER;
            case ANCHOR_DEFENSE_NEEDED : return COMM_TYPE.AMPLIFIER;
            // case OCCUPIED_ISLAND : return COMM_TYPE.ISLAND;
            case OUR_ISLAND: return COMM_TYPE.OUR_ISLANDS;
            case UNOCCUPIED_ISLAND : return COMM_TYPE.NEUTRAL_ISLANDS;
            case COMBAT_LOCATION : return COMM_TYPE.COMBAT;
            case CLOUD_COMBAT_LOCATION : return COMM_TYPE.COMBAT;
            case ARRAY_HEAD : break;
        }
        assert false;
        return null;
    }



    ////////////////////////////////////////
    // INITIALIZATION STUFF ////////////////
    ////////////////////////////////////////
    
    private static void allocateChannels(COMM_TYPE type, int channelCount) throws GameActionException{
        if (channelCount + channelsUsed > TOTAL_CHANNELS_COUNT)
            throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Too many channels being used");
        if (channelCount == 0) return;
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
        for (int i = START_CHANNEL_BANDS; i < START_CHANNEL_BANDS + MAX_HEADQUARTERS_CHANNELS_COUNT; i += CHANNELS_COUNT_PER_HEADQUARTER){
            SHAFlag flag = readSHAFlagType(i);
            if (flag == SHAFlag.EMPTY_MESSAGE){
                writeSHAFlagMessage(headquarterLoc, SHAFlag.HEADQUARTER_LOCATION, i);
                commsHeadquarterCount = ((i - START_CHANNEL_BANDS)/CHANNELS_COUNT_PER_HEADQUARTER) + 1;
                BuilderWrapper.setHeadquarterIndex(i + 2);
                break;
            }
            else if (flag == SHAFlag.HEADQUARTER_LOCATION){
                commsHeadquarterCount = (i - START_CHANNEL_BANDS)/CHANNELS_COUNT_PER_HEADQUARTER + 1;
                continue;
            }
            assert false;
        }
    }

    private static void headquarterChannelsInit() throws GameActionException{
        if (rc.getType() == RobotType.HEADQUARTERS && rc.getRoundNum() == 1){
            wipeCountChannels();
            // wipeScoreChannels();
            scoreChannelsInit();
            rc.writeSharedArray(ELIXIR_WELL_LOCATION_CHANNEL, 0);
            writeHeadquarterLocation(rc.getLocation());
            return;
        }
        int headquarterCount = getHeadquartersCount();
        channelsUsed += headquarterCount * 3;
    }

    public static void initCommunicationsArray() throws GameActionException{
        channelsUsed = START_CHANNEL_BANDS;
        commsHeadquarterCount = -1;
        commsEnemyHeadquarterCount = 0;
        commsHeadquarterLocations = null;
        commsEnemyHeadquarterLocations = null;
        locationsToWrite = null;
        locationsToWriteFlags = null;
        wrapAround = false;
        savedLocationsCount = 0;
        locationsToWipe = null;
        locationsToWipeFlags = null;
        wipeWrapAround = false;
        locationsToWipeCount = 0;
        headquarterChannelsInit();
        if (rc.getRoundNum() == 1) return;
        allocateChannels(COMM_TYPE.WELLS, WELLS_CHANNELS_COUNT);
        allocateChannels(COMM_TYPE.OUR_ISLANDS, OUR_ISLANDS_CHANNELS_COUNT);
        allocateChannels(COMM_TYPE.NEUTRAL_ISLANDS, NEUTRAL_ISLAND_CHANNELS_COUNT);
        allocateChannels(COMM_TYPE.AMPLIFIER, AMPLIFIER_CHANNELS_COUNT);
        allocateChannels(COMM_TYPE.COMBAT, COMBAT_CHANNELS_COUNT);
    }

    private static void scoreChannelsInit() throws GameActionException{
        wipeChannel(LAUNCHER_SCORE_CHANNEL);
        wipeChannel(CARRIER_SCORE_CHANNEL);
        wipeChannel(AMPLIFIER_SCORE_CHANNEL);
        // wipeChannel(STANDARD_ANCHOR_SCORE_CHANNEL);
        rc.writeSharedArray(STANDARD_ANCHOR_SCORE_CHANNEL, SimpleBuilder.INIT_ANCHOR_SCORE);
        // wipeChannel(BOOSTER_SCORE_CHANNEL);
        // wipeChannel(DESTABILIZER_SCORE_CHANNEL);
        // wipeChannel(ACCELERATING_ANCHOR_COUNT_CHANNEL);
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

    private static void writeAnchorCount(int count, int channel) throws GameActionException{
        int message = rc.readSharedArray(channel);
        if (count > 0) message |= 0x1;
        else message &= 0xFFFE;
        message &= (0x07FF);
        message |= (count << (SHAFlagBits + resourcePrioritizationBits));
        rc.writeSharedArray(channel, message);
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
        if (flag != SHAFlag.COLLECT_ANCHOR) rc.writeSharedArray(channel, (message << SHAFLAG_BITLENGTH) | flag.ordinal());
        else writeAnchorCount(message, channel);
    }

    public static void writeEnemyHeadquarterLocation(MapLocation loc) throws GameActionException{
        boolean empty = false;
        if (rc.getRoundNum() == 1) assert false;
        if (!rc.canWriteSharedArray(0, 0)) return;
        int headquarterCount = getHeadquartersCount();
        if (commsEnemyHeadquarterCount == headquarterCount)
            return;
        if (commsEnemyHeadquarterLocations == null)
            commsEnemyHeadquarterLocations = createNullMapLocations(headquarterCount);

        for (int i = COMM_TYPE.HEADQUARTER.channelStart + 1; i < COMM_TYPE.HEADQUARTER.channelStop; i += CHANNELS_COUNT_PER_HEADQUARTER){
            int message = rc.readSharedArray(i);
            SHAFlag flag = readSHAFlagFromMessage(message);
            if (flag == SHAFlag.ENEMY_HEADQUARTER_LOCATION && loc.equals(readLocationFromMessage(message))){
                commsEnemyHeadquarterCount = Math.max((i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER + 1, commsEnemyHeadquarterCount);
                if (commsEnemyHeadquarterLocations[(i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER].x == -1)
                    commsEnemyHeadquarterLocations[(i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER] = loc;
                return;
            }
            else if (flag == SHAFlag.ENEMY_HEADQUARTER_LOCATION && empty)
                throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "shouldn't have happened");
            else if (flag == SHAFlag.ENEMY_HEADQUARTER_LOCATION){
                if (commsEnemyHeadquarterLocations[(i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER].x == -1)
                    commsEnemyHeadquarterLocations[(i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER] = readLocationFromMessage(message);
                commsEnemyHeadquarterCount = Math.max((i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER + 1, commsEnemyHeadquarterCount);
                if (commsEnemyHeadquarterCount > getHeadquartersCount())
                    throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "enemy hq count. How???");
                continue;
            }
            else if (flag == SHAFlag.EMPTY_MESSAGE && !empty){
                empty = true;
                if (((i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER) < commsEnemyHeadquarterCount)
                    throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "enemy hq count. How2222???");
                commsEnemyHeadquarterCount = (i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER + 1;
                if (commsEnemyHeadquarterCount > getHeadquartersCount())
                    throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "enemy hq count. How???");
                writeSHAFlagMessage(loc, SHAFlag.ENEMY_HEADQUARTER_LOCATION, i);
                commsEnemyHeadquarterLocations[(i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER] = loc;
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
     *      When COMM_TYPE is WELLS   : ~200 at max
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
     *      When commType is WELLS   : ~200 at max
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
     *      When commType is WELLS   : ~200 at max
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
     *      When commType is WELLS   : ~200 at max
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
     *      When commType is WELLS   : ~200 at max
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
     *      When commType is WELLS   : ~200 at max
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

    public static void writeElixirWellLocation(MapLocation loc) throws GameActionException{
        int message = rc.readSharedArray(ELIXIR_WELL_LOCATION_CHANNEL);
        assert ((message & 0x8) >> 3) == 0;
        writeSHAFlagMessage(loc, SHAFlag.ELIXIR_WELL_LOCATION, ELIXIR_WELL_LOCATION_CHANNEL);
    }



    ////////////////////////////////////////
    // BUILD ORDER STUFF ///////////////////
    ////////////////////////////////////////

    /** Updates the count of the robot to increment by 1.
     * @param robotType : One of RobotType.HEADQUARTER, RobotType.CARRIER
     * @param channel : the channel to which the head needs to be updated.
     * @BytecodeCost ~90
     */
    public static void incrementRobotCount(RobotType robotType) throws GameActionException{
        if (!rc.canWriteSharedArray(0, 0)) return;
        int channel = getRobotCountChannel(robotType);
        rc.writeSharedArray(channel, rc.readSharedArray(channel) + 1);
    }

    public static void resetRobotCount(RobotType robotType) throws GameActionException{
        if (!rc.canWriteSharedArray(0, 0)) return;
        rc.writeSharedArray(getRobotCountChannel(robotType), 0);
    }

    public static void writeScore(RobotType type, int score) throws GameActionException{
        if (!rc.canWriteSharedArray(0, 0)) return;
        rc.writeSharedArray(getRobotScoreChannel(type), score);
    }

    public static void writeScore(Anchor anchor, int score) throws GameActionException{
        if (!rc.canWriteSharedArray(0, 0)) return;
        rc.writeSharedArray(getRobotScoreChannel(anchor), score);
    }

    private static int getRobotCountChannel(RobotType robotType) throws GameActionException{
        switch(robotType){
            case LAUNCHER: return LAUNCHER_COUNT_CHANNEL;
            case AMPLIFIER: return AMPLIFIER_COUNT_CHANNEL;
            default: break;
        }
        assert false;
        return -1;
    }

    public static int getRobotCount(RobotType robotType) throws GameActionException{
        return rc.readSharedArray(getRobotCountChannel(robotType));
    }

    private static int getRobotScoreChannel(RobotType type) throws GameActionException{
        switch(type){
            case CARRIER: return CARRIER_SCORE_CHANNEL;
            case AMPLIFIER: return AMPLIFIER_SCORE_CHANNEL;
            case LAUNCHER: return LAUNCHER_SCORE_CHANNEL;
            case BOOSTER: return BOOSTER_SCORE_CHANNEL;
            // case DESTABILIZER: return DESTABILIZER_SCORE_CHANNEL;
            default: assert false;
        }
        assert false;
        return -1;
    }

    private static int getRobotScoreChannel(Anchor anchor) throws GameActionException{
        switch(anchor){
            case STANDARD: return STANDARD_ANCHOR_SCORE_CHANNEL;
            // case ACCELERATING: return ACCELERATING_ANCHOR_COUNT_CHANNEL;
            default: assert false;
        }
        assert false;
        return -1;
    }

    public static int getRobotScore(RobotType robotType) throws GameActionException{
        return rc.readSharedArray(getRobotScoreChannel(robotType));
    }

    public static int getRobotScore(Anchor anchor) throws GameActionException{
        return rc.readSharedArray(getRobotScoreChannel(anchor));
    }

    private static RESOURCE_PRIORITIZATION getResourcePrioritization(ResourceType resourceType){
        switch(resourceType){
            case ADAMANTIUM: return RESOURCE_PRIORITIZATION.ADAMANTIUM;
            case MANA: return RESOURCE_PRIORITIZATION.MANA;
            default: break;
        }
        assert false;
        return null;
    }

    public static void writePrioritizedResource(ResourceType resourceType, int channel) throws GameActionException{
        // if (!rc.canWriteSharedArray(0, 0)) return;
        int message = rc.readSharedArray(channel);
        int p = (message & 0x0006) >> 1;
        if (p == 0){
            message |= getResourcePrioritization(resourceType).ordinal() << 1;
            rc.writeSharedArray(channel, message);
            return;
        }
        p = (message & 0x0018) >> 3;
        if (p == 0){
            message |= getResourcePrioritization(resourceType).ordinal() << 3;
            rc.writeSharedArray(channel, message);
            return;
        }
        p = (message & 0x0060) >> 5;
        if (p == 0){
            message |= getResourcePrioritization(resourceType).ordinal() << 5;
            rc.writeSharedArray(channel, message);
            return;
        }
        p = (message & 0x0180) >> 7;
        if (p == 0){
            message |= getResourcePrioritization(resourceType).ordinal() << 7;
            rc.writeSharedArray(channel, message);
            return;
        }
        p = (message & 0x0600) >> 9;
        if (p == 0){
            message |= getResourcePrioritization(resourceType).ordinal() << 9;
            rc.writeSharedArray(channel, message);
            return;
        }
        assert false;
    }

    public static void writePrioritizationResources(ResourceType[] resources, int count, int channel) throws GameActionException{
        int message = rc.readSharedArray(channel);
        message &= 0xF801;
        while(count-- > 0)
            message |= (resources[count].resourceID << (SHAFlagBits + count*2));
        rc.writeSharedArray(channel, message);
    }

    public static void resetPrioritizationBits(int channel) throws GameActionException{
        int message = rc.readSharedArray(channel);
        message &= 0xF801;
        rc.writeSharedArray(channel, message);
    }

    private static ResourceType getResourceType(RESOURCE_PRIORITIZATION rPrioritization){
        switch(rPrioritization){
            case ADAMANTIUM: return ResourceType.ADAMANTIUM;
            case MANA: return ResourceType.MANA;
            default: break;
        }
        assert false;
        return null;
    }

    public static ResourceType getPrioritizedResource(int channel, int index) throws GameActionException{
        int message = rc.readSharedArray(channel);
        int p = (message & 0x0006) >> 1;
        if (p != 0){
            if (rc.canWriteSharedArray(0, 0))
                rc.writeSharedArray(channel, (message & 0xFFF9));
            return getResourceType(resourcePrioritizations[p]);
        }
        
        p = (message & 0x0018) >> 3;
        if (p != 0){
            if (rc.canWriteSharedArray(0, 0))
                rc.writeSharedArray(channel, (message & 0xFFE7));
            return getResourceType(resourcePrioritizations[p]);
        }
        
        p = (message & 0x0060) >> 5;
        if (p != 0){
            if (rc.canWriteSharedArray(0, 0))
                rc.writeSharedArray(channel, (message & 0xFF9F));
            return getResourceType(resourcePrioritizations[p]);
        }
        
        p = (message & 0x0180) >> 7;
        if (p != 0){
            if (rc.canWriteSharedArray(0, 0))
                rc.writeSharedArray(channel, (message & 0xFE7F));
            return getResourceType(resourcePrioritizations[p]);
        }
        
        p = (message & 0x0600) >> 9;
        if (p != 0){
            if (rc.canWriteSharedArray(0, 0))
                rc.writeSharedArray(channel, (message & 0xF9FF));
            return getResourceType(resourcePrioritizations[p]);
        }
        int hqCount = getHeadquartersCount();
        if (index + 1 < hqCount)
            return getPrioritizedResource(BotCarrier.initSpawningHeadquarterIndex(index + 1), index + 1);
        assert false : "message:" + message + " channel:" + channel + "; rn: " + rc.getRoundNum() + "; curLoc: " + rc.getLocation() + "; hqCount: " + hqCount + "; index: " + index;
        return null;
        // int message = rc.readSharedArray(channel);
        // return getResourceType(resourcePrioritizations[(message >> 1) & 0x3]);
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

    public static void wipeCountChannels() throws GameActionException{
        wipeChannel(LAUNCHER_COUNT_CHANNEL);
        wipeChannel(AMPLIFIER_COUNT_CHANNEL);
    }
    
    public static void wipeScoreChannels() throws GameActionException{
        wipeChannel(LAUNCHER_SCORE_CHANNEL);
        wipeChannel(CARRIER_SCORE_CHANNEL);
        wipeChannel(AMPLIFIER_SCORE_CHANNEL);
        wipeChannel(STANDARD_ANCHOR_SCORE_CHANNEL);
        // wipeChannel(BOOSTER_SCORE_CHANNEL);
        // wipeChannel(DESTABILIZER_SCORE_CHANNEL);
        // wipeChannel(ACCELERATING_ANCHOR_COUNT_CHANNEL);
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
     * @param type COMM_TYPE: WELLS, ISLAND, AMPLIFIER, or COMBAT
     * @throws GameActionException
     */
    public static void wipeChannelsCOMMTYPE(COMM_TYPE type) throws GameActionException{
        for (int i = type.channelStart; i < type.channelStop; ++i)
            wipeChannel(i);
        type.channelHead = type.channelStart;
        writeSHAFlagMessage(type.channelHead, SHAFlag.ARRAY_HEAD, type.channelHeadArrayLocationIndex);
    }

    public static void wipeChannelsSHAFlag(COMM_TYPE type, SHAFlag flag) throws GameActionException{
        for (int i = type.channelStart; i < type.channelStop; ++i){
            if (readSHAFlagFromMessage(rc.readSharedArray(i)) == flag)
                wipeChannel(i);
        }
    }


    public static void wipeThisLocationFromChannels(COMM_TYPE type, SHAFlag flag, MapLocation loc) throws GameActionException{
        if (!rc.canWriteSharedArray(0, 0)) return;
        if (loc == null) return;
        for (int i = type.channelStart; i < type.channelStop; ++i){
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) == flag && readLocationFromMessage(message).equals(loc)){
                rc.writeSharedArray(i, 0);
                continue;
            }
        }
    }



    ////////////////////////////////////////
    // FIND AND GET METHODS ////////////////
    ////////////////////////////////////////

    public static MapLocation getElixirWellTarget() throws GameActionException{
        int message = rc.readSharedArray(ELIXIR_WELL_LOCATION_CHANNEL);
        if (SHAFlags[message & 0x7] == SHAFlag.ELIXIR_WELL_LOCATION) return readLocationFromMessage(message);
        return null;
    }

    public static boolean findIfElixirWellMade() throws GameActionException{
        int message = rc.readSharedArray(ELIXIR_WELL_LOCATION_CHANNEL);
        if (SHAFlags[message & 0x7] == SHAFlag.ELIXIR_WELL_LOCATION && (message & 0x8) >> 3 == 1) return true;
        return false; 
    }

    /**
     * Gets the count of headquarters from the shared array
     * @BytecodeCost ~10.
     */
    public static int getHeadquartersCount() throws GameActionException{
        if (commsHeadquarterCount != -1 && rc.getRoundNum() > 1) return commsHeadquarterCount;
        MapLocation[] tempLocs = new MapLocation[GameConstants.MAX_STARTING_HEADQUARTERS];
        int count = 0;
        for (int i = START_CHANNEL_BANDS; i < START_CHANNEL_BANDS + MAX_HEADQUARTERS_CHANNELS_COUNT; i += CHANNELS_COUNT_PER_HEADQUARTER){
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) == SHAFlag.HEADQUARTER_LOCATION){
                count++;
                if (commsHeadquarterLocations == null) 
                    tempLocs[(i - START_CHANNEL_BANDS)/CHANNELS_COUNT_PER_HEADQUARTER] = readLocationFromMessage(message);
            }
            else break;
        }
        if ((count <= 0 && rc.getRoundNum() > 1) || count > 4)
            throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "headquarter count can't be this. count: " + Integer.toString(count));
        if (rc.getRoundNum() == 1){
            return count;
        }
        commsHeadquarterCount = count;
        COMM_TYPE.HEADQUARTER.channelStart = START_CHANNEL_BANDS;
        COMM_TYPE.HEADQUARTER.channelStop = START_CHANNEL_BANDS + count * CHANNELS_COUNT_PER_HEADQUARTER - 1;
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

    public static int getHeadquarterIndex(MapLocation hqLoc) throws GameActionException{
        MapLocation[] hqLocations = getAlliedHeadquartersLocationsList();
        for (int i = 0; i < hqLocations.length; ++i)
            if (hqLocations[i].equals(hqLoc)) return i;
        assert false;
        return -1;
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
            commsHeadquarterLocations[(i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER] = readLocationFromMessage(message);
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

    /**
     * Finds the kth (1 indexed) nearest headquarter to the current robot.
     * @param k : 1 indexed kth nearest headquarter. That is k = 1 is the nearest headquarter.
     * @return the kth nearest headquarter to the current robot
     * @throws GameActionException
     */
    public static MapLocation findKthNearestHeadquarter(int k) throws GameActionException{
        if (rc.getRoundNum() == 1) assert false;
        int headquarterCount = getHeadquartersCount();
        if (k > headquarterCount) return null;
        if (sortedHeadquarters != null) return sortedHeadquarters[k-1];
        sortedHeadquarters = new MapLocation[headquarterCount];
        currentLocation = rc.getLocation();
        MapLocation[] headquarters = getAlliedHeadquartersLocationsList();
        switch(headquarterCount){
            case 4: sortedHeadquarters[3] = headquarters[3];
            case 3: sortedHeadquarters[2] = headquarters[2];
            case 2: sortedHeadquarters[1] = headquarters[1];
            case 1: sortedHeadquarters[0] = headquarters[0]; break;
            default: assert false;
        }
        for (int i = 0; i < headquarterCount; ++i){
            for (int j = i + 1; j < headquarterCount; ++j){
                if (currentLocation.distanceSquaredTo(sortedHeadquarters[i]) > currentLocation.distanceSquaredTo(sortedHeadquarters[j])){
                    MapLocation temp = sortedHeadquarters[i];
                    sortedHeadquarters[i] = sortedHeadquarters[j];
                    sortedHeadquarters[j] = temp;
                }
            }
        }
        return sortedHeadquarters[k-1];
    }

    public static MapLocation[] getEnemyHeadquartersLocationsList() throws GameActionException{
        int headquarterCount = getHeadquartersCount();
        if (commsEnemyHeadquarterCount > headquarterCount)
            throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "enemy hq count moar. how?");
        if (commsEnemyHeadquarterCount == headquarterCount){
            if (commsEnemyHeadquarterLocations == null) assert false;
            return commsEnemyHeadquarterLocations;
        }
        if (commsEnemyHeadquarterLocations == null)
            commsEnemyHeadquarterLocations = createNullMapLocations(headquarterCount);
        // System.out.println("reached here!");
        for (int i = COMM_TYPE.HEADQUARTER.channelStart + 1; i < COMM_TYPE.HEADQUARTER.channelStop; i += CHANNELS_COUNT_PER_HEADQUARTER){
            if (commsEnemyHeadquarterLocations[(i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER].x != -1) continue;
            int message = rc.readSharedArray(i);
            SHAFlag flag = readSHAFlagFromMessage(message);
            if (flag == SHAFlag.EMPTY_MESSAGE) 
                return commsEnemyHeadquarterLocations;
            if (flag != SHAFlag.ENEMY_HEADQUARTER_LOCATION)
                throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "not enemy hq location. No!");
            commsEnemyHeadquarterLocations[(i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER] = readLocationFromMessage(message);
            commsEnemyHeadquarterCount = (i - COMM_TYPE.HEADQUARTER.channelStart)/CHANNELS_COUNT_PER_HEADQUARTER + 1;
        }
        return commsEnemyHeadquarterLocations;
    }

    public static MapLocation findNearestEnemyHeadquarterLocation() throws GameActionException{
        if (rc.getRoundNum() < 2) assert false;
        MapLocation[] enemyHeadquarters = getEnemyHeadquartersLocationsList();
        if (enemyHeadquarters == null) return CENTER_OF_THE_MAP;
        MapLocation optLoc = null, myLoc = rc.getLocation();
        int optDist = -1, enemyHeadquarterCount = getHeadquartersCount(), curDist;
        for (int i = 0; i < enemyHeadquarterCount; ++i){
            if (enemyHeadquarters[i] == null || enemyHeadquarters[i].x == -1) 
                break;
            curDist = myLoc.distanceSquaredTo(enemyHeadquarters[i]);
            if (optLoc == null || curDist < optDist){
                optLoc = enemyHeadquarters[i];
                optDist = curDist;
            }
        }
        if (optLoc != null)
            return optLoc;
        else
            return CENTER_OF_THE_MAP;
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
     * @BytecodeCost<pre>
     *      When COMM_TYPE is WELLS   : ~150 at max
     *When COMM_TYPE is ISLAND : ~150 at max
     *When COMM_TYPE is COMBAT : ~500 at max
     * </pre>
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

    /**
     * Function to check whether a location is already present in channels.
     * @param loc : location to be checked. Another function present to check if a message is already there in the channels
     * @param type : communication band to be checked in: WELLS, ISLANDS, COMBAT
     * @param flag : SHAFlag of the message
     * @return true or false
     * @throws GameActionException
     * @BytecodeCost<pre>
     *      When COMM_TYPE is WELLS   : ~20 at max
     *When COMM_TYPE is ISLAND : ~20 at max
     *When COMM_TYPE is COMBAT : ~64 at max
     * </pre>
     */
    public static boolean findIfLocationAlreadyPresent(MapLocation loc, COMM_TYPE type, SHAFlag flag) throws GameActionException{
        for (int i = type.channelStart; i < type.channelStop; ++i){
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) == flag && readLocationFromMessage(message).equals(loc))
                return true;
        }
        return false;
    }

    /**
     * Function to check whether a message is already present in channels.
     * @param message : message to be checked. Another function present to check if a location is already there in the channels
     * @param type : communication band to be checked in: WELLS, ISLANDS, COMBAT
     * @return true or false
     * @throws GameActionException
     * @BytecodeCost<pre>
     *      When COMM_TYPE is WELLS   : ~20 at max
     *When COMM_TYPE is ISLAND : ~20 at max
     *When COMM_TYPE is COMBAT : ~64 at max
     * </pre>
     */
    public static boolean findIfMessageAlreadyPresent(int message, COMM_TYPE type) throws GameActionException{
        for (int i = type.channelStart; i < type.channelStop; ++i){
            if (rc.readSharedArray(i) == message)
                return true;
        }
        return false;
    }

    public static boolean findIfAnchorProduced(MapLocation hqLoc) throws GameActionException{
        int hqIdx = getHeadquarterIndex(hqLoc);
        int message = rc.readSharedArray(COMM_TYPE.HEADQUARTER.channelStart + hqIdx * CHANNELS_COUNT_PER_HEADQUARTER + 2);
        if ((message & 0x1) == 1){
            BotCarrier.collectAnchorHQidx = COMM_TYPE.HEADQUARTER.channelStart + hqIdx * CHANNELS_COUNT_PER_HEADQUARTER + 2;
            return true;
        }
        return false;
    }

    public static int getAnchorCount(int channel) throws GameActionException{
        int message = rc.readSharedArray(channel);
        return ((message & 0xF800) >> 11);
    }



    ////////////////////////////////////////
    // SURVEY METHODS //////////////////////
    ////////////////////////////////////////

    /**
     * Survey the vision radius for unoccupied islands and write them to the communication channels.
     * @throws GameActionException
     * @BytecodeCost ~ 300 - 400
     */
    public static void surveyForIslands() throws GameActionException{
        // if (!rc.canWriteSharedArray(0, 0) && rc.getRoundNum() < 3) return;
        boolean canWrite = rc.canWriteSharedArray(0,0);
        COMM_TYPE type = COMM_TYPE.NEUTRAL_ISLANDS;
        int[] islandIDs = rc.senseNearbyIslands();
        MapLocation[] locations;
        if (islandIDs.length == 0) return;
        for (int i = islandIDs.length; i-->0;){
            if (rc.senseTeamOccupyingIsland(islandIDs[i]) != Team.NEUTRAL) continue;
            locations = rc.senseNearbyIslandLocations(islandIDs[i]);
            if (canWrite)
                writeAndOverwriteLesserPriorityMessage(type, locations[0], SHAFlag.UNOCCUPIED_ISLAND);
            else saveThisLocation(locations[0], SHAFlag.UNOCCUPIED_ISLAND);
        }
    }

    /**
     * Survey the vision radius for unoccupied islands and write them to the communication channels.
     * @throws GameActionException
     * @BytecodeCost ~ 300 - 400
     */
    public static void surveyForIslandsAmplifiers() throws GameActionException{
        if (rc.getRoundNum() < 2) return;
        COMM_TYPE type = COMM_TYPE.NEUTRAL_ISLANDS;
        int[] islandIDs = rc.senseNearbyIslands();
        MapLocation[] locations;
        if (islandIDs.length == 0) return;
        for (int i = islandIDs.length; i-->0;){
            if (rc.senseTeamOccupyingIsland(islandIDs[i]) != Team.NEUTRAL) {
                locations = rc.senseNearbyIslandLocations(islandIDs[i]);
                Comms.wipeThisLocationFromChannels(type, SHAFlag.UNOCCUPIED_ISLAND, locations[0]);
            }
            else{
                locations = rc.senseNearbyIslandLocations(islandIDs[i]);
                writeAndOverwriteLesserPriorityMessage(type, locations[0], SHAFlag.UNOCCUPIED_ISLAND);
            }
        }
    }
    

    ////////////////////////////////////////
    // SAVING UNWRITTEN LOCATIONS //////////
    ////////////////////////////////////////

    public static void writeOrSaveLocation(MapLocation loc, SHAFlag flag) throws GameActionException{
        if (rc.canWriteSharedArray(0, 0)){
            COMM_TYPE type = getCOMM_TYPEFromSHAFlag(flag);
            if (!findIfLocationAlreadyPresent(loc, type, flag))
            writeAndOverwriteLesserPriorityMessage(type, loc, flag);
        }
        else saveThisLocation(loc, flag);
    }

    public static boolean checkIfLocationSaved(MapLocation loc){
        int count = OVERWRITE_OLDER_SAVED_LOCATIONS && wrapAround ? 20 : savedLocationsCount;
        switch(count){
            case 20:
                if (locationsToWrite[19].equals(loc)) return true;
            case 19:
                if (locationsToWrite[18].equals(loc)) return true;
            case 18:
                if (locationsToWrite[17].equals(loc)) return true;
            case 17:
                if (locationsToWrite[16].equals(loc)) return true;
            case 16:
                if (locationsToWrite[15].equals(loc)) return true;
            case 15:
                if (locationsToWrite[14].equals(loc)) return true;
            case 14:
                if (locationsToWrite[13].equals(loc)) return true;
            case 13:
                if (locationsToWrite[12].equals(loc)) return true;
            case 12:
                if (locationsToWrite[11].equals(loc)) return true;
            case 11:
                if (locationsToWrite[10].equals(loc)) return true;
            case 10:
                if (locationsToWrite[9].equals(loc)) return true;
            case 9:
                if (locationsToWrite[8].equals(loc)) return true;
            case 8:
                if (locationsToWrite[7].equals(loc)) return true;
            case 7:
                if (locationsToWrite[6].equals(loc)) return true;
            case 6:
                if (locationsToWrite[5].equals(loc)) return true;
            case 5:
                if (locationsToWrite[4].equals(loc)) return true;
            case 4:
                if (locationsToWrite[3].equals(loc)) return true;
            case 3:
                if (locationsToWrite[2].equals(loc)) return true;
            case 2:
                if (locationsToWrite[1].equals(loc)) return true;
            case 1:
                if (locationsToWrite[0].equals(loc)) return true;
            case 0: return false;
            default: break;
        }
        assert false;
        return false;
    }

    public static void saveThisLocation(MapLocation loc, SHAFlag flag) throws GameActionException{
        if (locationsToWrite == null){
            locationsToWrite = new MapLocation[MAX_LOCATIONS_TO_SAVE];
            locationsToWriteFlags = new SHAFlag[MAX_LOCATIONS_TO_SAVE];
            savedLocationsCount = 0;
        }
        if (savedLocationsCount >= MAX_LOCATIONS_TO_SAVE){
            if (!OVERWRITE_OLDER_SAVED_LOCATIONS) return;
            wrapAround = true;
            savedLocationsCount = 0;
        }
        
        if (checkIfLocationSaved(loc)) return;

        if (findIfLocationAlreadyPresent(loc, getCOMM_TYPEFromSHAFlag(flag), flag)) return;
        if (BotCarrier.DEBUG_PRINT)
            System.out.println("Saved location: " + loc + " with flag: " + flag);
        locationsToWrite[savedLocationsCount] = loc;
        locationsToWriteFlags[savedLocationsCount++] = flag;
    }

    public static boolean writeSavedLocations() throws GameActionException{
        if (locationsToWrite == null || savedLocationsCount == 0) return true;
        if (!rc.canWriteSharedArray(0, 0)) return false;
        if (Clock.getBytecodesLeft() < 500) return false;
        if (saveElixirWellMade){
            writeElixirWellMade();
            saveElixirWellMade = false;
        }
        int count = OVERWRITE_OLDER_SAVED_LOCATIONS && wrapAround ? 20 : savedLocationsCount;
        while(count-->0){
            COMM_TYPE type = getCOMM_TYPEFromSHAFlag(locationsToWriteFlags[count]);
            assert type != null : "Invalid SHAFlag";
            assert type != COMM_TYPE.HEADQUARTER : "Invalid SHAFlag";
            if (findIfLocationAlreadyPresent(locationsToWrite[count], type, locationsToWriteFlags[count])) continue;
            if (BotCarrier.DEBUG_PRINT)
                System.out.println("Writing saved location: " + locationsToWrite[count] + " with flag: " + locationsToWriteFlags[count]);
            writeAndOverwriteLesserPriorityMessage(type, locationsToWrite[count], locationsToWriteFlags[count]);
            if (Clock.getBytecodesLeft() < 500) {
                savedLocationsCount = count;
                wrapAround = false;
                return false;
            }
        }
        savedLocationsCount = 0;
        wrapAround = false;
        return true;
    }

    private static void writeElixirWellMade() throws GameActionException{
        int message = rc.readSharedArray(ELIXIR_WELL_LOCATION_CHANNEL);
        message |= 0x8;
        rc.writeSharedArray(ELIXIR_WELL_LOCATION_CHANNEL, message);
    }

    public static void saveOrWriteThatElixirWellMade() throws GameActionException{
        if (rc.canWriteSharedArray(0, 0)) writeElixirWellMade();
        saveElixirWellMade = true;
    }

    

    ////////////////////////////////////////
    // SAVING LOCATIONS TO WIPE ////////////
    ////////////////////////////////////////

    public static void wipeOrSaveLocation(MapLocation loc, SHAFlag flag) throws GameActionException{
        if (rc.canWriteSharedArray(0, 0)){
            COMM_TYPE type = getCOMM_TYPEFromSHAFlag(flag);
            wipeThisLocationFromChannels(type, flag, loc);
        }
        else saveThisLocationToWipe(loc, flag);
    }

    public static boolean checkIfLocationSavedToWipe(MapLocation loc){
        int count = OVERWRITE_OLDER_WIPED_LOCATIONS && wipeWrapAround ? 10 : locationsToWipeCount;
        switch(count){
            case 10:
                if (locationsToWipe[9].equals(loc)) return true;
            case 9:
                if (locationsToWipe[8].equals(loc)) return true;
            case 8:
                if (locationsToWipe[7].equals(loc)) return true;
            case 7:
                if (locationsToWipe[6].equals(loc)) return true;
            case 6:
                if (locationsToWipe[5].equals(loc)) return true;
            case 5:
                if (locationsToWipe[4].equals(loc)) return true;
            case 4:
                if (locationsToWipe[3].equals(loc)) return true;
            case 3:
                if (locationsToWipe[2].equals(loc)) return true;
            case 2:
                if (locationsToWipe[1].equals(loc)) return true;
            case 1:
                if (locationsToWipe[0].equals(loc)) return true;
            case 0: return false;
            default: break;
        }
        assert false;
        return false;
    }

    public static void saveThisLocationToWipe(MapLocation loc, SHAFlag flag) throws GameActionException{
        if (locationsToWipe == null){
            locationsToWipe = new MapLocation[MAX_LOCATIONS_TO_WIPE];
            locationsToWipeFlags = new SHAFlag[MAX_LOCATIONS_TO_WIPE];
            locationsToWipeCount = 0;
        }
        if (locationsToWipeCount >= MAX_LOCATIONS_TO_WIPE){
            if (!OVERWRITE_OLDER_WIPED_LOCATIONS) return;
            wipeWrapAround = true;
            locationsToWipeCount = 0;
        }
        
        if (checkIfLocationSavedToWipe(loc)) return;

        if (!findIfLocationAlreadyPresent(loc, getCOMM_TYPEFromSHAFlag(flag), flag)) return;
        if (BotCarrier.DEBUG_PRINT)
            System.out.println("Saved location: " + loc + " with flag: " + flag);
        locationsToWipe[locationsToWipeCount] = loc;
        locationsToWipeFlags[locationsToWipeCount++] = flag;
    }

    public static boolean wipeSavedLocations() throws GameActionException{
        if (locationsToWipe == null || locationsToWipeCount == 0) return true;
        if (!rc.canWriteSharedArray(0, 0)) return false;
        if (Clock.getBytecodesLeft() < 500) return false;
        int count = OVERWRITE_OLDER_WIPED_LOCATIONS && wipeWrapAround ? 10 : locationsToWipeCount;
        while(count-->0){
            SHAFlag flag = locationsToWipeFlags[count];
            COMM_TYPE type = getCOMM_TYPEFromSHAFlag(flag);
            assert type != null : "Invalid SHAFlag";
            assert type != COMM_TYPE.HEADQUARTER : "Invalid SHAFlag";
            // if (!findIfLocationAlreadyPresent(locationsToWrite[count], type, locationsToWriteFlags[count])) continue;
            // if (BotCarrier.DEBUG_PRINT)
            //     System.out.println("Writing saved location: " + locationsToWrite[count] + " with flag: " + locationsToWriteFlags[count]);
            wipeThisLocationFromChannels(type, flag, locationsToWipe[count]);
            if (Clock.getBytecodesLeft() < 500) {
                locationsToWipeCount = count;
                wipeWrapAround = false;
                return false;
            }
        }
        locationsToWipeCount = 0;
        wipeWrapAround = false;
        return true;
    }
}
