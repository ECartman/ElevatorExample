package com.microsoft.jfx.test;

public class ElevatorMemory {
    private static final ElevatorMemory SINGLETON_MEMORY = new ElevatorMemory();
    public static final byte TOP_FLOOR = 10; // <-- JAVA Style const.
    public static final byte LOWEST_FLOOR = 1; // <-- JAVA Style const. NOTE if we change this Value we need to change some of the Calculations to calculate the value relative to the first position

    private byte floor = LOWEST_FLOOR;

    private short Memory = 0;//16bits

    private ElevatorMemory() {
        floor = LOWEST_FLOOR;
        Memory = 0;
    }

    public short getMemoryValue() {
        return Memory;
    }

    public short StoreAndMemoryValue(short Value) {
        return Memory &= Value;
    }

    public short StoreorMemoryValue(short Value) {
        return Memory |= Value;
    }

    public short RemoveDestinationFlag(short Flag) {
        return StoreXorMemoryValue(Flag);
    }

    public boolean RemoveCurrentFloorFlag() {
        if (AreWeOnDestination()) {
            StoreXorMemoryValue((short) (1 << (floor - LOWEST_FLOOR)));
            return true;
        }
        return false;
    }

    public short StoreXorMemoryValue(short Value) {
        return Memory ^= Value;
    }

    public short TestAndMemoryValue(short Value) {
        return (short) (Memory & Value);
    }

    public short TestXorMemoryValue(short Value) {
        return (short) (Memory ^ Value);
    }

    public short clearMemory() {
        Memory = 0;
        return Memory;
    }

    public static ElevatorMemory getElevatorMemory() {
        return SINGLETON_MEMORY;
    }

    public void setFloor(byte floor) {
        if (floor <= TOP_FLOOR) {
            this.floor = floor;
        } else {
            // You FOOL! you are sending ppl to THE MOON!!!!!!
            throw new IndexOutOfBoundsException(floor);
        }
    }
    /**
     * returns the Current floor Number. 
     * if you need the Flag call {@link #getFloorFlagForNumber(byte)} and sent the floor Byte.
     * 
     * @return a Byte that contains the NUMERIC value for the current floor the Elevator is in. 
     */
    public byte getFloor() {
        return floor;
    }

    public byte getNextFloorNumber() {
        short FlagUpper = 0, FlagLower = 0;
        for (int index = floor, indexd = floor; index <= TOP_FLOOR || indexd >= LOWEST_FLOOR; index++, indexd--) {
            FlagUpper = TestAndMemoryValue((short) (1 << (index - LOWEST_FLOOR)));
            FlagLower = TestAndMemoryValue((short) (1 << (indexd - LOWEST_FLOOR)));
            // if found a destination return it.
            if (FlagUpper != 0)
                return (byte) index;
            if (FlagLower != 0)
                return (byte) indexd;
        }
        // at this point none found. so no next to go. the caller should make sure if
        // there is any location to go.
        return 0;
    }

    public short getNextFloorFlag() {
        short FlagUpper = 0, FlagLower = 0;
        //divide and Conquer aporach with no recursion. it returns the first upper floor per cicle. otherwise returns the first lower per cicle. 
        for (int index = floor, indexd = floor; index <= TOP_FLOOR || indexd >= LOWEST_FLOOR; index++, indexd--) {
            FlagUpper = TestAndMemoryValue((short) (1 << (index - LOWEST_FLOOR)));
            FlagLower = TestAndMemoryValue((short) (1 << (indexd - LOWEST_FLOOR)));
            // if found a destination return it.
            if (FlagUpper != 0)
                return FlagUpper;
            if (FlagLower != 0)
                return FlagLower;
        }
        // at this point none found. so no next to go. the caller should make sure if
        // there is any location to go.
        return 0;
    }

    /**
     * NOTE: we only check if we have where to go. NOT whever the Memory is purged
     * nor purge it for you.
     * 
     * @return true if there are other floors to visit Besides the "current" one.
     */
    public boolean hasWhereToMove() {
        if (AreWeOnDestination()) {
            short pendingFlags = TestXorMemoryValue(getFloorFlagForNumber(floor));
            if (pendingFlags != 0) {
                return true;
            }
        } else if (Memory != 0) {
            return true;
        }
        return false;
    }

    /**
     * check if Current Floor is the one on the Destination. 
     * @return
     */
    public boolean AreWeOnDestination() {
       short currentFloorFlag = getFloorFlagForNumber(floor); 
       //TestAndMemoryValue(currentFloorFlag) == currentFloorFlag;
        return (Memory & currentFloorFlag) == currentFloorFlag;
    }

    /**
     * returns the 16bit Flag for the provided floor. 
     * by bitshifting 1 to the Floor position. 
     * minus the "First" floor position.
     * @param floor the Number(ed) floor position to go
     * @return 16bit(short) value that contains the Flag
     */
    public static short getFloorFlagForNumber(byte floor) {
        return (short) (1 << (floor-LOWEST_FLOOR));
    }
   /**
     * check whenever the provided Floor Flag is already Selected by 
     * doing an "AND" operation. if the result of this operation is the same 
     * as the provided Flag. Then the value is equals and therefore returns true
     * @param floorflag the Flag to Compare
     * @return True if the provided Flag is alredy on Elevator Memory, false otherwise. 
     */
    public boolean checkifAlredyFlipped(short floorflag) {
        return TestAndMemoryValue(floorflag)== floorflag;
    }

    /**
     * check whenever the provided Floor Number is already Selected by 
     * Converting the Number into a Flag (by calling {@link #getFloorFlagForNumber(byte)})
     * and then  Calling {@link #checkifAlredyFlipped(short)}
     * @param floorflag the Floor Number to check
     * @return True if the provided Floor is alredy on Elevator Memory, false otherwise. 
     */
    public boolean checkifFloorSelected(byte floornum) {
        short flag = ElevatorMemory.getFloorFlagForNumber(floornum);
        return TestAndMemoryValue(flag)== flag;
    }

    /**
     * how many bits more exist on the memory (lenght) compared agast the provided lenght (of bits)
     * @param length the lenght of bits to compare agast this class defined. 
     * @return the ammount of bits of diference we have on the memory or 0 if the diference is negative or 0.
     */
    public static int HowManyFillerBits(int length) {
       int floorcount=(ElevatorMemory.TOP_FLOOR-(ElevatorMemory.LOWEST_FLOOR-1));
        if(length<floorcount){
            return floorcount-length;
        }
        else 
            return 0;
    }

}
