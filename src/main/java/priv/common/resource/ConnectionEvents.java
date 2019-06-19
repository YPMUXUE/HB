package priv.common.resource;

public enum  ConnectionEvents {
    BIND2(0x00),
    LOGIN(0x01),
    CLOSE(0xcafe),
    CONNECTION_ESTABLISH(0xAC),
    CONNECTION_ESTABLISH_FAILED(0xAD),
    CONNECT(0x1995),
    BIND(0x0320)
    ;
     ConnectionEvents(int code){
        this.code=(short) code;
    }
    public final short code;
     public short getCode(){
         return this.code;
     }
}
