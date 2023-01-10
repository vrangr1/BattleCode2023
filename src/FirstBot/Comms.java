package FirstBot;

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
    private static final int MAXIMUM_HEADQUARTERS_CHANNELS = 12;
    public static final int CHANNELS_COUNT_PER_HEADQUARTER = 3;
    private static final int WELLS_CHANNELS_COUNT = 10;
    private static final int COMBAT_CHANNELS_COUNT = 32;
    // ununsed channels count: 10 (in the case of max count of headquarters (i.e. 4))
    private static final int TOTAL_CHANNELS_COUNT = COMMS_VARCOUNT; // 64
    private static int channelsUsed = 0;
    private static int commsHeadquarterCount = -1;
    


    ////////////////////////////////////////
    // ENUMS: //////////////////////////////
    ////////////////////////////////////////
    
    // SHAFlags written in increasing order of priority. Unsure of NOT_A_LOCATION_MESSAGE placement in the priority order.
    public static enum SHAFlag { // reserving 4 bits for this
        EMPTY_MESSAGE,                      // 0x0
        
        // HEADQUARTERS' MESSAGES' TYPES
        HEADQUARTER_LOCATION,               // 0x1
        ENEMY_HEADQUARTER_LOCATION,         // 0x2
        HEADQUARTER_MESSAGE,                // 0x3

        // WELLS CHANNELS MESSAGES' TYPES
        ADAMANTIUM_WELL_LOCATION,           // 0x4
        MANA_WELL_LOCATION,                 // 0x5
        ELIXIR_WELL_LOCATION,               // 0x6

        // COMBAT CHANNELS MESSAGES' TYPES
        COMBAT_LOCATION,                    // 0x7
        ENEMY_ADAMANTIUM_WELL_LOCATION,     // 0x8
        ENEMY_MANA_WELL_LOCATION,           // 0x9
        ENEMY_ANCHOR_LOCATION,              // 0xA
        ENEMY_ELIXIR_ANCHOR_LOCATION,       // 0xB
        ENEMY_ELIXIR_WELL_LOCATION,         // 0xC
        ANCHOR_DEFENSE_NEEDED,              // 0xD

        // COMMS UTILITY TYPE
        ARRAY_HEAD;                         // 0xE
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
        HEADQUARTER;    // 0x3

        public int channelStart, channelStop, channelHead, channelHeadArrayLocationIndex;
    }



    ////////////////////////////////////////
    // GENERAL UTILITY METHODS /////////////
    ////////////////////////////////////////

    public static int getNumChannelsUsed(){
        return channelsUsed;
    }
    
    /**
     * Gets the count of headquarters from the shared array
     * @BytecodeCost ~10.
     */
    public static int getHeadquartersCount() throws GameActionException{
        if (commsHeadquarterCount != -1 && rc.getRoundNum() > 10) return commsHeadquarterCount;
        int count = 0;
        for (int i = 0; i < channelsUsed; i += 3){
            if (readSHAFlagFromMessage(i) == SHAFlag.HEADQUARTER_LOCATION)
                count++;
            else break;
        }
        if ((count <= 0 && rc.getRoundNum() > 1) || count > 4)
            throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "headquarter count can't be this.");
        commsHeadquarterCount = count;
        COMM_TYPE.HEADQUARTER.channelStart = 0;
        COMM_TYPE.HEADQUARTER.channelStop = count * CHANNELS_COUNT_PER_HEADQUARTER - 1;
        return count;
    }



    ////////////////////////////////////////
    // PRIVATE UTILITY METHODS /////////////
    ////////////////////////////////////////
    
    private static void incrementHead(COMM_TYPE type){
        type.channelHead++;
        type.channelHead = Math.max(type.channelStart, type.channelHead % type.channelStop);
    }

    private static int incrementHead(int i, COMM_TYPE type){
        return Math.max(type.channelStart, (i + 1) % type.channelStop);
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
        writeSHAFlagMessage(type.channelHead, SHAFlag.ARRAY_HEAD, type.channelHeadArrayLocationIndex);
    }

    public static void allocateHeadquartersChannels(int channelCount, MapLocation headquarterLoc) throws GameActionException{
        if (channelCount + channelsUsed > TOTAL_CHANNELS_COUNT)
            throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Too many channels being used");
        writeSHAFlagMessage(headquarterLoc, SHAFlag.HEADQUARTER_LOCATION, channelsUsed);
        channelsUsed += channelCount;
    }

    private static void botCommsInit() throws GameActionException{
        switch(rc.getType()){
            case HEADQUARTERS: BotHeadquarters.initComms(); return;
            default: break;
        }
        int headquarterCount = getHeadquartersCount();
        channelsUsed += headquarterCount * 3;
    }

    public static void initCommunicationsArray() throws GameActionException{
        channelsUsed = 0;
        commsHeadquarterCount = -1;
        botCommsInit();
        allocateChannels(COMM_TYPE.WELLS, WELLS_CHANNELS_COUNT);
        allocateChannels(COMM_TYPE.COMBAT, COMBAT_CHANNELS_COUNT);
    }



    ////////////////////////////////////////
    // READ METHODS ////////////////////////
    ////////////////////////////////////////

    public static SHAFlag readSHAFlagFromMessage(int message){
        return SHAFlags[message & 0xF];
    }

    public static SHAFlag readSHAFlagType(int sharedArrayIndex) throws GameActionException{
        int message = rc.readSharedArray(sharedArrayIndex);
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

    /** Writes 0 to the given channel.
     * @BytecodeCost at least 75
     */
    public static void writeSHAFlagMessage(MapLocation loc, SHAFlag flag, int channel) throws GameActionException{
        int value = intFromMapLocation(loc);
        rc.writeSharedArray(channel, (value << SHAFLAG_BITLENGTH) | flag.ordinal());
    }

    // Mostly for non-location type messages
    public static void writeSHAFlagMessage(int message, SHAFlag flag, int channel) throws GameActionException{
        rc.writeSharedArray(channel, (message << SHAFLAG_BITLENGTH) | flag.ordinal());
    }

    public static void writeEnemyHeadquarterLocation(MapLocation loc) throws GameActionException{
        boolean empty = false;
        for (int i = COMM_TYPE.HEADQUARTER.channelStart + 1; i < COMM_TYPE.HEADQUARTER.channelStop; i += 3){
            int message = rc.readSharedArray(i);
            SHAFlag flag = readSHAFlagFromMessage(message);
            if (flag == SHAFlag.ENEMY_HEADQUARTER_LOCATION && loc == readLocationFromMessage(message))
                return;
            else if (flag == SHAFlag.ENEMY_HEADQUARTER_LOCATION && empty)
                throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "shouldn't have happened");
            else if (flag == SHAFlag.EMPTY_MESSAGE && !empty){
                empty = true;
                writeSHAFlagMessage(loc, SHAFlag.ENEMY_HEADQUARTER_LOCATION, i);
                return;
            }
            else if (flag == SHAFlag.ENEMY_HEADQUARTER_LOCATION) continue;
            else assert false : "logical error in writeEnemyHeadquarterLocation func";
        }
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



    ////////////////////////////////////////
    // FIND METHODS ////////////////////////
    ////////////////////////////////////////

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
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) == flag){
                return readLocationFromMessage(message);
            }
            i = incrementHead(i, type);
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
            int message = rc.readSharedArray(i);
            if (readSHAFlagFromMessage(message) == flag){
                channel = i;
                loc = readLocationFromMessage(message);
                break;
            }
            i = incrementHead(i, type);
        }while(i != type.channelHead);
        
        if (channel != -1) wipeChannel(channel);
        return loc;
    }
}