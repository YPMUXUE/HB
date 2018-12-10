package common.resource;

public enum  ConnectionEvents {
    RECONNECT_IF_NECESSARY(0x00), USE_OLD_CONNECTION(0x00),OPEN_CONNECTION(0x0320),ONE_OFF_CONNECTION(0x1995),NEW_KEEP_CONNECTION(0xcafe);
     ConnectionEvents(int code){
        this.code=code;
    }
    public final int code;
     public int getCode(){
         return this.code;
     }
}
